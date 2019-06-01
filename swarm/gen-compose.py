#!/usr/bin/env python3

import json
import sys
import argparse

base_dict = {}

base_dict["version"] = "3"

class Node:

    def __init__(self, incr_value):
        self.incr_value = incr_value
        self.template = json.loads(open("broker-template.json", "r").read())

    def populate_node_name(self):

        node_name = "rabbit" + str(self.incr_value + 1)
        self.template["hostname"] = node_name
        port_list = ["127.0.0.1:"+str(15672 + int(self.incr_value)+1)+":15672"]
        self.template["ports"] = port_list

        return self.template

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Specifications to generate compose file")

    parser.add_argument(
        "-w",
        "--workers",
        action="store",
        dest="workers",
        type=int,
        help="No. of workers to deploy in the swarm cluster",
        )

    parser.add_argument(
        "-p",
        "--proxies",
        action='store',
        dest="proxies",
        type=int,
        help="No. of HTTP proxies to deploy in the swarm cluster",
        )

    args = parser.parse_args()

    workers = int(args.workers)
    proxies = int(args.proxies)

    rabbit_list = ["rabbit" + str(i) for i in range(1,workers+1)]

    node_list = [Node(node).populate_node_name() for node in range(0, int(workers))]

    base_dict = json.loads(open("compose-template.json", "r").read())

    counter = 1

    for node in node_list:
        
        base_dict["services"]["rabbit"+str(counter)] = node
        
        counter = counter + 1

    base_dict["services"]["vertx"]["deploy"]["replicas"] = int(proxies)
    #base_dict["services"]["vertx"]["depends_on"] = rabbit_list + ["postgres"]
    #base_dict["services"]["dbconnector"]["depends_on"] = rabbit_list + ["elk"]
    #base_dict["services"]["unbind-daemon"]["depends_on"] = rabbit_list + ["postgres"]

    f = open("docker-compose.json", "w")
    f.write(json.dumps(base_dict))
    f.close()
