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

def get_updates(settings):
    updates = {}

    cluster_time = settings['settings']['cluster']['timestamp']
    cluster_ts = settings['settings']['cluster']['timestamp_in_millis']

    for node_id, node in settings['settings']['nodes'].iteritems():
        file_ts = node['file_timestamp_in_millis']
        file_time = node['file_timestamp']
        for key, setting in node['inconsistencies'].iteritems():
            if not setting['_updatable']:
                continue # not updatable
            if file_ts <= cluster_ts:
                continue # file is older
            update = updates.setdefault(key, {})
            update['_effective'] = { 'value': setting['effective'], 'ts': cluster_ts, 'time': cluster_time }
            update[node_id] = { 'value': setting['desired'], 'ts': file_ts, 'time': file_time }

    return updates

def get_update_decisions(updates):
    update_request = {}
    for key, nodes in updates.iteritems():
        effective = nodes.pop('_effective')
        values = set([v['value'] for v in nodes.values()])
        if len(values) > 1:
            sorted_nodes = sorted(nodes.iteritems(), key=lambda o: o[1]['ts'])
            explanation = ['%s:[%s]%s' % (node_id, node['time'], '(none)' if node['value'] is None else node['value']) for node_id, node in sorted_nodes]
            if effective['value'] is None:
                effective['value'] = '(none)'
            print 'No unanimity in uptodate value for %s: effective:[%s]%s, %s' % (key, effective['time'], effective['value'], ', '.join(explanation))
            continue
        value = values.pop()
        print 'Update', key, 'from', effective['value'], 'to', value
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
    updates = get_updates(settings)
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
