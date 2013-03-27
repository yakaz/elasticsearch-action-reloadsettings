#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import requests
import json
import sys

def parse():
    parser = argparse.ArgumentParser(description='Reloads ElasticSearch cluster settings', conflict_handler='resolve')
    parser.add_argument('-h', '--host',  action='store',       type=str,   default='localhost', dest='host',     help='Host to contact')
    parser.add_argument('-p', '--port',  action='store',       type=int,   default=9200,        dest='port',     help='Port to contact')
    parser.add_argument('-l', '--local', action='store_true',                                   dest='local',    help='Only query the local, contacted node')
    parser.add_argument('-n', '--dry-run', '--simulate',
                                         action='store_true',                                   dest='simulate', help='Do not apply any update')
    args = parser.parse_args()
    args.update_url = 'http://%s:%d/_cluster/settings' % ( args.host, args.port )
    args.reload_url = 'http://%s:%d/_nodes%s/settings/reload' % ( args.host, args.port, '/_local' if args.local else '' )
    return args

def raise_with_answer_text(request, message=None):
    if request.status_code != requests.codes.ok:
        if message is not None:
            if request.text is not None:
                print message, request.text
            else:
                print message
        elif request.text is not None:
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

def get_updates(settings, node_local_inconsistencies):
    updates = {}

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

def format_multiple_values(source):
    sorted_source = sorted(source.iteritems(), key=lambda o: o[1]['ts'])
    explanation = ['%s:[%s]%s' % (node_id, node['time'], '(none)' if node['value'] is None else node['value']) for node_id, node in sorted_source]
    return ', '.join(explanation)

def get_update_decisions(updates):
    update_request = {}
    for key, nodes in updates.iteritems():
        effective = nodes.pop('_effective')
        values = set([v['value'] for v in nodes.values()])
        if len(values) > 1:
            if 'inconsistent' in effective:
                print 'No unanimity in uptodate value for %s: effective(from %s): %s, desired: %s' % (key, effective['from'], format_multiple_values(effective['inconsistent']), format_multiple_values(nodes))
            else:
                if effective['value'] is None:
                    effective['value'] = '(none)'
                print 'No unanimity in uptodate value for %s: effective(from %s): [%s]%s, desired: %s' % (key, effective['from'], effective['time'], effective['value'], format_multiple_values(nodes))
            continue
        value = values.pop()
        if effective['from'] is None:
            effective_str = '(unset)'
        else:
            if 'inconsistent' in effective:
                effective['value'] = format_multiple_values(effective['inconsistent'])
            if effective['value'] is None:
                effective['value'] = '(none)'
            effective_str = '(%s) %s' % (effective['from'], effective.get('value'))
        print 'Update %s from %s to %s' % (key, effective_str, value)
        update_request[key] = value
    return update_request

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
        print 'Updated:', r.text

def main():
    args = parse()
    settings = get_settings(args)
    local_inconsistencies = collect_node_local_inconsistencies(settings)
    updates = get_updates(settings, local_inconsistencies)
    update_decisions = get_update_decisions(updates)
    if has_update_decisions(update_decisions):
        apply_update_decisions(args, update_decisions)
        if args.simulate:
            print 'Would check'
        else:
            print 'Checking'
            if has_update_decisions(get_update_decisions(get_updates(get_settings(args)))):
                print 'Some updates are still to be performed!'
            else:
                print 'OK'
    else:
        print 'Nothing to do'



if __name__ == '__main__':
    main()
