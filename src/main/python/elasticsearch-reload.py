#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import requests
import json
import sys

def parse(argv = None, **kwargs):
    parser = argparse.ArgumentParser(description='Reloads ElasticSearch cluster settings', conflict_handler='resolve')
    parser.add_argument('-h', '--host',  action='store',       type=str,   default='localhost', dest='host',     help='Host to contact')
    parser.add_argument('-p', '--port',  action='store',       type=int,   default=9200,        dest='port',     help='Port to contact')
    parser.add_argument('-l', '--local', action='store_true',                                   dest='local',    help='Only query the local, contacted node')
    parser.add_argument('-n', '--dry-run', '--simulate',
                                         action='store_true',                                   dest='simulate', help='Do not apply any update')
    parser.add_argument('-c', '--check', '--just-check',  action='store_true',                  dest='just_check', help='Just check if no updates are to be done and exit')
    parser.add_argument('--die-on-conflicts',
                                         action='store_true',                                   dest='die_on_conflicts', help='Exit with error if any conflicts are found')
    parser.add_argument('--resolve-simple-conflicts',
                                         action='store_true',                                   dest='resolve_simple_conflicts', help='Resolve simple conflicts')
    if argv is None:
        argv = sys.argv[1:]
    argv.extend(['--%s' % key.replace('_', '-') for key, value in kwargs.iteritems() if value == True])
    argv.extend(['--%s=%s' % (key.replace('_', '-'), value) for key, value in kwargs.iteritems() if type(value) != bool])
    args = parser.parse_args(argv)
    args.update_url = 'http://%s:%d/_cluster/settings' % ( args.host, args.port )
    args.reload_url = 'http://%s:%d/_nodes%s/settings/reload' % ( args.host, args.port, '/_local' if args.local else '' )
    return args

def raise_with_answer_text(request, message=None):
    if request.status_code != requests.codes.ok:
        if message is not None:
            if request.text is not None and len(request.text) > 0:
                print message, request.text
            else:
                print message
        elif request.text is not None and len(request.text) > 0:
            print request.text
    request.raise_for_status()

def get_settings(args):
    r = requests.get(args.reload_url)
    raise_with_answer_text(r, 'Could not get settings!')
    return r.json

def collect_node_local_inconsistencies(settings):
    local_inconsistencies = set()

    cluster_ts = settings['settings']['cluster']['timestamp_in_millis']

    for node_id, node in settings['settings']['nodes'].iteritems():
        file_ts = node['file_timestamp_in_millis']
        for key, setting in node['inconsistencies'].iteritems():
            if not setting['_updatable']:
                continue # not updatable
            if file_ts <= cluster_ts:
                continue # file is older
            local_inconsistencies.add(key)

    return local_inconsistencies

def has_node_local_inconsistencies(node_local_inconsistencies):
    return len(node_local_inconsistencies) > 0

def get_updates(settings, node_local_inconsistencies=None):
    updates = {}
    if node_local_inconsistencies is None:
        node_local_inconsistencies = collect_node_local_inconsistencies(settings)

    cluster_ts = settings['settings']['cluster']['timestamp_in_millis']
    cluster_time = settings['settings']['cluster']['timestamp']

    oldest_file_ts = None
    oldest_file_time = None
    for node_id, node in settings['settings']['nodes'].iteritems():
        file_ts = node['file_timestamp_in_millis']
        file_time = node['file_timestamp']
        if file_ts > oldest_file_ts:
            oldest_file_ts = file_ts
            oldest_file_time = file_time

    for key in node_local_inconsistencies:
        update = updates[key] = {}
        from_cluster = key in settings['settings']['cluster']['effective']
        consistent   = key in settings['settings']['consistencies']['effective']
        inconsistent = key in settings['settings']['inconsistencies']['effective']
        inconsistencies = {}
        for node_id, node in settings['settings']['nodes'].iteritems():
            file_ts = node['file_timestamp_in_millis']
            file_time = node['file_timestamp']
            if file_ts <= cluster_ts:
                continue # file is older
            update[node_id] = { 'value': node['desired'].get(key), 'ts': file_ts, 'time': file_time }
            if inconsistent:
                inconsistencies[node_id] = { 'value': node['effective'].get(key), 'ts': file_ts, 'time': file_time }
        if from_cluster:
            update['_effective'] = { 'from': 'cluster', 'value': settings['settings']['cluster']['effective'][key], 'ts': cluster_ts, 'time': cluster_time }
        elif consistent:
            update['_effective'] = { 'from': 'nodes', 'value': settings['settings']['consistencies']['effective'][key], 'ts': oldest_file_ts, 'time': oldest_file_time }
        elif inconsistent:
            update['_effective'] = { 'from': 'nodes', 'inconsistent': inconsistencies }
        else:
            update['_effective'] = { 'from': None, 'value': None, 'ts': 0, 'time': None }

    return updates

def sort_multiple_values_by_timestamp(source):
    return sorted(source.iteritems(), key=lambda o: o[1]['ts'])

def resolve_simple_confict(key, source, effective):
    if effective['from'] is None:
        # Cannot resolve when no effective value
        return None
    if 'inconsistent' in effective:
        # Cannot resolve with an inconsistent effective value
        return None
    sorted_source = sort_multiple_values_by_timestamp(source)
    if sorted_source[0][1]['value'] != effective['value']:
        # Cannot resolve when the oldest value is not consistent with the effective value
        return None
    last = None
    changes = []
    for node_id, value in sort_multiple_values_by_timestamp(source):
        if value['value'] != last:
            last = value['value']
            changes.append(value['value'])
    if len(changes) > 2:
        # Updated nodes desire the effective value, and more than one other value (eg.: effective['value'], foo, bar, baz)
        # or some updated nodes require the effective value but some previous updated node require another one (eg.: effective['value'], foo, effective['value'])
        return None
    resolved = changes[-1] # resolve to the most updated value (can be effective['value'] if len(changes)==1, but this should not happen
    print 'Resolved simple conflict for %s to %s: %s, desired: %s' % (key, resolved, format_effective_values(effective), format_multiple_values(source))
    return resolved

def format_multiple_values(source):
    sorted_source = sort_multiple_values_by_timestamp(source)
    explanation = ['%s:[%s]%s' % (node_id, node['time'], '(none)' if node['value'] is None else node['value']) for node_id, node in sorted_source]
    return ', '.join(explanation)

def format_effective_values(effective):
    if effective['from'] is None:
        return 'effective:(unset)'
    if 'inconsistent' in effective:
        return 'effective(from %s): %s' % (effective['from'], format_multiple_values(effective['inconsistent']))
    if effective['value'] is None:
        effective['value'] = '(none)'
    return 'effective(from %s): [%s]%s' % (effective['from'], effective['time'], effective['value'])

def get_update_decisions(args, updates):
    conflicts = []
    update_request = {}
    for key, nodes in updates.iteritems():
        effective = nodes.pop('_effective')
        values = set([v['value'] for v in nodes.values()])
        if len(values) > 1:
            resolved = None
            if args.resolve_simple_conflicts:
                resolved = resolve_simple_confict(key, nodes, effective)
            if resolved is not None:
                values = [resolved]
            else:
                print 'No unanimity in uptodate value for %s: %s, desired: %s' % (key, format_effective_values(effective), format_multiple_values(nodes))
                conflicts.append(key)
                continue
        value = values.pop()
        print 'Update %s from %s to %s' % (key, format_effective_values(effective), value)
        update_request[key] = value
    return (conflicts, update_request)

def has_conflicts(conflicts):
    return len(conflicts) > 0

def has_update_decisions(update_decisions):
    return len(update_decisions) > 0

def apply_update_decisions(args, update_decisions):
    if not has_update_decisions(update_decisions):
        return
    update_request = json.dumps({ 'transient': update_decisions })
    if args.simulate:
        print 'Would PUT', args.update_url, 'with data', update_request
    else:
        r = requests.put(args.update_url, data=update_request)
        raise_with_answer_text(r, 'Could not update!')
        print 'Updated%s' % (': %s' % (r.text,) if r.text is not None and len(r.text) > 0 else '')

def reload_settings(argv = None, **kwargs):
    args = parse(argv, **kwargs)
    settings = get_settings(args)
    local_inconsistencies = collect_node_local_inconsistencies(settings)
    if not has_node_local_inconsistencies(local_inconsistencies):
        print 'Nothing to do'
        return 0
    updates = get_updates(settings, local_inconsistencies)
    conflicts, update_decisions = get_update_decisions(args, updates)
    if args.die_on_conflicts and has_conflicts(conflicts):
        print 'There are some conflicts, dying'
        return 1
    if not has_update_decisions(update_decisions):
        print 'Nothing can be done'
        return 0
    if args.just_check:
        print 'Nothing was applied, just checked'
        return 1
    apply_update_decisions(args, update_decisions)
    if args.simulate:
        print 'Would check'
    else:
        print 'Checking'
        conflicts, update_decisions = get_update_decisions(args, get_updates(get_settings(args)))
        if args.die_on_conflicts and has_conflicts(conflicts):
            print 'Some settings are now conflicting, dying'
            return 2
        if has_update_decisions(update_decisions):
            print 'Some updates are still to be performed!'
            return 2
        else:
            print 'OK'
    return 0

def main():
    return reload_settings(sys.argv[1:])



if __name__ == '__main__':
    sys.exit(main())
