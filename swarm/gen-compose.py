#!/usr/bin/env python3

import json
import sys

base_dict = {}

base_dict["version"] = "3"

class Node:

    def __init__(self, incr_value):
        self.incr_value = incr_value
        self.template = json.loads(open("broker-template.json", "r").read())

    def populate_node_name(self):

        node_name = "rabbit" + str(self.incr_value + 1)
        self.template["hostname"] = node_name
        port_list = [str(15672 + int(self.incr_value)+1)+":15672"]
        self.template["ports"] = port_list

        return self.template

if __name__ == "__main__":

    workers, proxies = sys.argv[1].split(":")
    
    node_list = [Node(node).populate_node_name() for node in range(0, int(workers))]

    base_dict = json.loads(open("compose-template.json", "r").read())

    counter = 1

    for node in node_list:
        
        base_dict["services"]["rabbit"+str(counter)] = node
        
        counter = counter + 1

    base_dict["services"]["vertx"]["deploy"]["replicas"] = int(proxies)

    f = open("docker-compose.json", "w")
    f.write(json.dumps(base_dict))
    f.close()
