#!/usr/bin/env python3

import json
import sys

base_dict = {}

base_dict["version"] = "3"

class Node:

    def __init__(self, incr_value):
        self.incr_value = incr_value
        self.template = """
{
  "image": "rabbitmq:3.7.14-management",

  "hostname": "xxx",

  "environment": {
          "RABBITMQ_DEFAULT_USER": "rabbitmq",
          "RABBITMQ_DEFAULT_PASS": "rabbitmq",
          "RABBITMQ_DEFAULT_VHOST": "/"
  },

  "ports": [
          "15672:15672",
          "5672:5672"
  ],

  "volumes": [
          "./enabled_plugins:/etc/rabbitmq/enabled_plugins"
  ],

  "networks": ["rabbit-net"],

  "deploy": {
    "placement": {
      "constraints": ["node.role == worker"] 
      }
    }
}
"""
        self.template_json = json.loads(self.template)

    def populate_node_name(self):

        node_name = "rabbit" + str(self.incr_value + 1)
        self.template_json["hostname"] = node_name
        port_list = [str(15672 + int(self.incr_value)+1)+":15672", str(5672 + int(self.incr_value)+1)+":5672"]
        self.template_json["ports"] = port_list

        return self.template_json

if __name__ == "__main__":

    managers, workers, proxies = sys.argv[1].split(":")
    
    node_list = [Node(node).populate_node_name() for node in range(0, int(workers))]

    base_dict["version"] = "3"

    base_dict["networks"] = {}
    base_dict["networks"]["rabbit-net"] = {}
    base_dict["networks"]["rabbit-net"]["driver"] = "overlay"

    proxy = """
{
	"image": "amqproxy",
	
        "deploy": {
	    "replicas": 1
	    },
	
        "ports": ["4673:5673"],

        "networks": ["rabbit-net"],

        "deploy": {
          "placement": {
            "constraints": ["node.role == manager"] 
            }
        }
}
"""

    proxy_json = json.loads(proxy)

    proxy_json["deploy"]["replicas"] = int(proxies)

    visualiser = """
{
	"image": "dockersamples/visualizer:stable",

	"ports": ["8080:8080"],

	"volumes": ["/var/run/docker.sock:/var/run/docker.sock"],

	"deploy": {
	    "placement": {
		"constraints": ["node.role == manager"]
	    }
	},

	"networks": ["rabbit-net"]
}

"""

    visualiser_json = json.loads(visualiser)


    counter = 1
    base_dict["services"] = {}

    for node in node_list:
        
        base_dict["services"]["rabbit"+str(counter)] = node
        
        counter = counter + 1

    base_dict["services"]["proxy"] = proxy_json
    base_dict["services"]["visualiser"] = visualiser_json
    
    f = open("docker-compose.json", "w")
    f.write(json.dumps(base_dict))
    f.close()
