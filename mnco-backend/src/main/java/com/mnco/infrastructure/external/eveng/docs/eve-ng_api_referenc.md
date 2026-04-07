How to use EVE-NG API
EVE-NG APIs use JSend, a JSON response in the following syntax:

{
"code": 404,
"message": "Requested folder does not exist (60008).",
"status": "fail"
}
Code is the HTTP response code, message is a simple string explaining what is going on, and status is a single word explaining the response. Five status type are used:

success for 20x HTTP codes;
unauthorized for 400 HTTP code, meaning that the user session has timed out;
unauthorized for 401 HTTP code, meaning that user should login;
forbidden for 403 HTTP code, meaning that user does not have enough privileges;
fail for other 40x HTTP codes;
error for 50x HTTP codes.
The default Web-UI uses only APIs, so this is an essential part to develop new Web-UI themes, integration and so on. Mind that each user can login from a single location only. If the same user login twice, the second login disable the first one.

EVE-NG Pro use ssl protocol. All samples here require adding -k and https as protocol… So adapt this documentation….

ADVICE:

Due to EVE-NG Code evolution, some call’s documentation are outdated ( more parameters supported ). It is advised to use the GET method to have a full set of parameters available. Use your browser developer tools ( network tab ) to spy API calls and have the latest API information using the headers, request and response…



Authentication
The following API requests are involved on login and logout process. All other API requests require an authenticated user.

Login
Community edition:

curl -s -b /tmp/cookie -c /tmp/cookie -X POST -d '{"username":"admin","password":"eve"}' http://127.0.0.1/api/auth/login

Pro Edition:

curl -k -s -b /tmp/cookie -c /tmp/cookie -X POST -d '{"username":"admin","password":"eve","html5":"0"}' https://127.0.0.1/api/auth/login

A successful login provides the following output:

{
"code": 200,
"message": "User logged in (90013).",
"status": "success"
}
User Info
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/auth

An authenticated user can get its own information:

{
"code": 200,
"data": {
"email": "root@localhost",
"folder": "/",
"lab": null,
"lang": "en",
"name": "EVE-NG Administrator",
"role": "admin",
"tenant": "0",
"username": "admin"
},
"message": "User has been loaded (90002).",
"status": "success"
}
Logout
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/auth/logout

All users can logout, this request cannot fail:

{
"code": 200,
"message": "User logged out (90019).",
"status": "success"
}
System status
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/status

An authenticated user can get system statistics:

{
"code": 200,
"data": {
"cached": 5,
"cpu": 1,
"disk": 31,
"dynamips": 0,
"iol": 0,
"mem": 8,
"qemu": 0,
"qemu_version": "2.4.0",
"swap": 0,
"version": "development"
},
"message": "Fetched system status (60001).",
"status": "success"
}
List node templates
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/list/templates/

An authenticated user can list all available node templates:

{
"code": 200,
"data": {
"a10": "A10 vThunder",
"acs": "Cisco ACS",
"asa": "Cisco ASA",
"asav": "Cisco ASAv",
"bigip": "F5 BIG-IP LTM VE",
"brocadevadx": "Brocade vADX",
"c1710": "Cisco IOS 1710 (Dynamips)",
"c3725": "Cisco IOS 3725 (Dynamips)",
"c7200": "Cisco IOS 7206VXR (Dynamips)",
"cda": "Cisco Context Directory Agent",
"cips": "Cisco IPS",
"clearpass": "Aruba ClearPass",
"coeus": "Cisco Web Security Appliance",
"cpsg": "CheckPoint Security Gateway VE",
"csr1000v": "Cisco CSR 1000V",
"cumulus": "Cumulus VX",
"extremexos": "ExtremeXOS",
"fortinet": "Fortinet FortiGate",
"hpvsr": "HP VSR1000",
"iol": "Cisco IOL",
"ise": "Cisco ISE",
"linux": "Linux",
"mikrotik": "MikroTik RouterOS",
"nsvpx": "Citrix Netscaler",
"olive": "Juniper Olive",
"ostinato": "Ostinato",
"paloalto": "Palo Alto VM-100 Firewall",
"sourcefire": "Cisco Sourcefire",
"sterra": "S-Terra",
"timos": "Alcatel 7750 SR",
"titanium": "Cisco NX-OSv (Titanium)",
"ucspe": "Cisco UCS-PE",
"veos": "Arista vEOS",
"vios": "Cisco vIOS",
"viosl2": "Cisco vIOS L2",
"vmx": "Juniper vMX",
"vnam": "Cisco vNAM",
"vsrx": "Juniper vSRX",
"vsrxng": "Juniper vSRX NextGen",
"vwaas": "Cisco vWAAS",
"vwlc": "Cisco vWLC",
"vyos": "VyOS",
"win": "Windows",
"xrv": "Cisco XRv"
},
"message": "Successfully listed node templates (60003).",
"status": "success"
}
A single template can be listed:

curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/list/templates/iol

All available images for the selected template will be included in the output:

{
"code": 200,
"data": {
"description": "Cisco IOL",
"options": {
"config": {
"list": {
"Saved": "Saved",
"Unconfigured": "Unconfigured"
},
"name": "Startup configuration",
"type": "list",
"value": "Unconfigured"
},
"delay": {
"name": "Delay (s)",
"type": "input",
"value": 0
},
"ethernet": {
"name": "Ethernet portgroups (4 int each)",
"type": "input",
"value": 1
},
"icon": {
"list": {
"Desktop.png": "Desktop",
"Firewall.png": "Firewall",
"Frame Relay.png": "Frame Relay",
"HUB.png": "HUB",
"Load Balancer.png": "Load Balancer",
"MPLS.png": "MPLS",
"Network Analyzer.png": "Network Analyzer",
"Router.png": "Router",
"Server.png": "Server",
"Switch L3.png": "Switch L3",
"Switch.png": "Switch",
"WAN Optimizer.png": "WAN Optimizer"
},
"name": "Icon",
"type": "list",
"value": "Router.png"
},
"image": {
"list": {
"L2-IPBASEK9-M-15.1-20130726.bin": "L2-IPBASEK9-M-15.1-20130726.bin",
"L3-ADVENTERPRISEK9-M-15.4-1T.bin": "L3-ADVENTERPRISEK9-M-15.4-1T.bin"
},
"name": "Image",
"type": "list",
"value": "L3-ADVENTERPRISEK9-M-15.4-1T.bin"
},
"name": {
"name": "Name/prefix",
"type": "input",
"value": "R"
},
"nvram": {
"name": "NVRAM",
"type": "input",
"value": 1024
},
"ram": {
"name": "RAM",
"type": "input",
"value": 256
},
"serial": {
"name": "Serial portgroups (4 int each)",
"type": "input",
"value": 1
}
},
"type": "iol"
},
"message": "Successfully listed node template (60032).",
"status": "success"
}
List network types
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/list/networks

An authenticated user can list all available network types:

{"code": 200,
"data": {
"bridge": "bridge",
"ovs": "ovs",
"pnet0": "pnet0",
"pnet1": "pnet1",
"pnet2": "pnet2",
"pnet3": "pnet3",
"pnet4": "pnet4",
"pnet5": "pnet5",
"pnet6": "pnet6",
"pnet7": "pnet7",
"pnet8": "pnet8",
"pnet9": "pnet9"
},
"message": "Successfully listed network types (60002).",
"status":"success"}
List user roles
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/list/roles

An authenticated user can list all user roles:

{
"code": 200,
"data": {
"admin": "Administrator",
"editor": "Editor",
"user": "User"
},
"message": "Successfully listed user roles (60041).",
"status": "success"
}
Navigating between folders
The following API requests allow to manages folders and labs as files.

List content inside a folder
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/folders/User1

An authenticated user can list what is inside a folder:

{
"code": 200,
"data": {
"folders": [
{
"name": "..",
"path": "/"
},
{
"name": "Folder 1",
"path": "/User1/Folder 1"
},
{
"name": "Folder 2",
"path": "/User1/Folder 2"
},
{
"name": "Folder 3",
"path": "/User1/Folder 3"
}
],
"labs": [
{
"file": "Lab 1.unl",
"path": "/User1/Lab 1.unl"
},
{
"file": "Lab 2.unl",
"path": "/User1/Lab 2.unl"
},
{
"file": "Lab 3.unl",
"path": "/User1/Lab 3.unl"
}
]
},
"message": "Successfully listed path (60007).",
"status": "success"
}
Folders and labs are listed using different arrays.

Add a new folder
curl -s -c /tmp/cookie -b /tmp/cookie
-X POST -d '{"path":"/User1/Folder 3","name":"New Folder"}' -H
'Content-type: application/json' http://127.0.0.1/api/folders

An authenticated user can add a folder inside a specific path:

{
"code": 200,
"message": "Folder has been created (60014).",
"status": "success"
}
Move/rename an existent folder
curl -s -c /tmp/cookie -b /tmp/cookie
-X PUT -d '{"path":"/User1/Folder 3/Test Folder"}' -H 'Content-type:
application/json'
http://127.0.0.1/api/folders/User1/Folder%203/New%20Folder

An authenticated user can add a folder inside a specific path:

{
"code": 200,
"message": "Folder moved (60049).",
"status": "success"
}
Delete an existing folder
curl -s -c /tmp/cookie -b /tmp/cookie
-X DELETE -H 'Content-type: application/json'
http://127.0.0.1/api/folders/User1/Folder%203/Test%20Folder

An authenticated user can delete an existing folder:

{
"code": 200,
"message": "Folder has been deleted (60012).",
"status": "success"
}
Managing users
The following API requests allow to manage EVE-NG users and permissions.

Get a user
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/users/

An authenticated user can get all EVE-NG users:

{
"code": 200,
"data": {
"admin": {
"email": "root@localhost",
"expiration": "-1",
"folder": "/User1",
"ip": "127.0.0.1",
"lab": null,
"name": "UNetLab Administrator",
"pexpiration": "-1",
"pod": "0",
"role": "admin",
"session": "1447319929",
"username": "admin"
},
"User1": {
"email": "User1.Lastname@gmail.com",
"expiration": "-1",
"folder": "/Featured/Cisco/Basic",
"ip": "192.168.19.1",
"lab": "/Featured/Cisco/Basic/STP.unl",
"name": "User1 Lastname",
"pexpiration": "-1",
"pod": "1",
"role": "admin",
"session": "1447319925",
"username": "User1"
}
},
"message": "Successfully listed users (60040).",
"status": "success"
}
A single user can be retrieved:

curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/users/admin

Here the output:

{
"code": 200,
"data": {
"email": "root@localhost",
"expiration": "-1",
"ip": "127.0.0.1",
"name": "UNetLab Administrator",
"pexpiration": "-1",
"pod": "0",
"role": "admin",
"session": "1447319929",
"username": "admin"
},
"message": "Successfully listed users (60040).",
"status": "success"
}
Add a new user
curl -s -c /tmp/cookie -b /tmp/cookie
-X POST -d '{"username":"testuser","name":"Test
User","email":"test@unetlab.com","password":"testpassword1","role":"user","expiration":"-1","pod":127,"pexpiration":"1451520000"}'
-H 'Content-type: application/json' http://127.0.0.1/api/users

An authenticated user can add a new EVE-NG user:

{
"code": 201,
"message": "User saved (60042).",
"status": "success"
}
Parameters:

email: the email address of the user;
expiration: date until the user is valid (UNIX timestamp) or -1 if never expires;
name: a description for the user, usually salutation;
password (mandatory): the user password used to login;
role: see “List user roles”;
username (mandatory): a unique alphanumeric string used to login;
Edit an user
curl -s -c /tmp/cookie -b /tmp/cookie
-X PUT -d '{"name":"New Test
User","email":"testuser@unetlab.com","password":"newpassword","role":"user","expiration":"1451520000","pod":127,"pexpiration":"-1"}'
-H 'Content-type: application/json' http://127.0.0.1/api/users/testuser

An authenticated user can edit an existent EVE-NG user:

{
"code": 200,
"message": "User saved (60042).",
"status": "success"
}
Parameters:

email: the email address of the user;
expiration: date until the user is valid (UNIX timestamp) or -1 if never expires;
name: a description for the user, usually salutation;
password: the user password used to login;
role: see “List user roles”;
Delete an user
curl -s -c /tmp/cookie -b /tmp/cookie -X DELETE -H 'Content-type: application/json' http://127.0.0.1/api/users/testuser

An authenticated user can delete an existent EVE-NG user:

{
"code": 201,
"message": "User saved (60042).",
"status": "success"
}
Managing labs
The following API requests allow to manage labs and object inside a lab, like nodes, networks… and so on.

Get a lab
curl -s -c /tmp/cookie -b /tmp/cookie -X GET -H 'Content-type: application/json' http://127.0.0.1/api/labs/User1/Lab%201.unl

An authenticated user can retrieve a lab:

{
"code": 200,
"data": {
"author": "User1 Lastname",
"body": "",
"description": "A new test lab.",
"filename": "Lab 1.unl",
"id": "d34628dd-cc1d-4e52-8f91-4a0673985d87",
"name": "Lab 1",
"version": "1"
},
"message": "Lab has been loaded (60020).",
"status": "success"
}
Get one or all networks configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/networks

An authenticated user can retrieve all configured networks in a lab:

{
"code": 200,
"data": {
"1": {
"id": 1,
"left": 409,
"name": "Net OVS",
"top": 345,
"type": "ovs"
},
"2": {
"id": 2,
"left": 583,
"name": "Net2",
"top": 261,
"type": "bridge"
},
"3": {
"id": 3,
"left": 256,
"name": "Net3",
"top": 276,
"type": "bridge"
},
"4": {
"id": 4,
"left": 583,
"name": "Net4",
"top": 483,
"type": "bridge"
},
"5": {
"id": 5,
"left": 409,
"name": "Net5",
"top": 491,
"type": "bridge"
}
},
"message": "Successfully listed networks (60004).",
"status": "success"
}
A single network can be retrieved:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/networks/1

Here the output:

{
"code": 200,
"data": {
"left": 409,
"name": "Net OVS",
"top": 345,
"type": "ovs"
},
"message": "Successfully listed network (60005).",
"status": "success"
}
Get all remote endpoint for both ethernet and serial interfaces
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/links

An authenticated user can retrieve all available endpoint in a lab:

{
"code": 200,
"data": {
"ethernet": {
"1": "Net OVS",
"2": "Net2",
"3": "Net3",
"4": "Net4",
"5": "Net5"
},
"serial": {
"3": {
"1": "R3 s1/0",
"17": "R3 s1/1",
"33": "R3 s1/2",
"49": "R3 s1/3"
},
"4": {
"1": "R4 s1/0",
"17": "R4 s1/1",
"33": "R4 s1/2",
"49": "R4 s1/3"
}
}
},
"message": "Fetced all lab networks and serial endpoints (60024).",
"status": "success"
}
This API is useful when a user need to connect a node.

Get one or all nodes configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes

An authenticated user can retrieve all configured nodes in a lab:

{
"code": 200,
"data": {
"1": {
"console": "telnet",
"cpu": 1,
"delay": 0,
"ethernet": 4,
"icon": "Router.png",
"id": 1,
"image": "vios-adventerprisek9-m-15.4-1.3.0.181",
"left": 358,
"name": "R1",
"ram": 512,
"status": 0,
"template": "vios",
"top": 330,
"type": "qemu",
"url": "telnet://127.0.0.1:32769",
"uuid": "ab60e9de-2599-4b67-919a-b769fb6e270d"
},
"2": {
"console": "telnet",
"cpu": 1,
"delay": 0,
"ethernet": 4,
"icon": "Router.png",
"id": 2,
"image": "vios-adventerprisek9-m-15.4-1.3.0.181",
"left": 501,
"name": "R2",
"ram": 512,
"status": 0,
"template": "vios",
"top": 330,
"type": "qemu",
"url": "telnet://127.0.0.1:32770",
"uuid": "206323a6-000b-40bc-a765-9c7e7e5751ee"
},
"3": {
"console": "telnet",
"delay": 0,
"ethernet": 1,
"icon": "Router.png",
"id": 3,
"image": "L3-ADVENTERPRISEK9-M-15.4-1T.bin",
"left": 430,
"name": "R3",
"nvram": 1024,
"ram": 256,
"serial": 1,
"status": 0,
"template": "iol",
"top": 529,
"type": "iol",
"url": "telnet://127.0.0.1:32771"
},
"4": {
"console": "telnet",
"delay": 0,
"ethernet": 1,
"icon": "Router.png",
"id": 4,
"image": "L3-ADVENTERPRISEK9-M-15.4-1T.bin",
"left": 430,
"name": "R4",
"nvram": 1024,
"ram": 256,
"serial": 1,
"status": 0,
"template": "iol",
"top": 192,
"type": "iol",
"url": "telnet://127.0.0.1:32772"
}
},
"message": "Successfully listed nodes (60026).",
"status": "success"
}
A single node can be retrieved:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1

Here the output:

{
"code": 200,
"data": {
"config": "Unconfigured",
"console": "telnet",
"cpu": 1,
"delay": 0,
"ethernet": 4,
"icon": "Router.png",
"image": "vios-adventerprisek9-m-15.4-1.3.0.181",
"left": 358,
"name": "R1",
"ram": 512,
"status": 0,
"template": "vios",
"top": 330,
"type": "qemu",
"url": "telnet://127.0.0.1:32769",
"uuid": "ab60e9de-2599-4b67-919a-b769fb6e270d"
},
"message": "Successfully listed node (60025).",
"status": "success"
}
Start one or all nodes configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/start

An authenticated user can start all configured nodes in a lab:

{
"code": 400,
"message": "Failed to start node (12).",
"status": "fail"
}
A single node can be started:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1/start

Here the output:

{
"code": 200,
"message": "Node started (80049).",
"status": "success"
}
Stop one or all nodes configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/stop

An authenticated user can stop all configured nodes in a lab:

{
"code": 200,
"message": "Nodes stopped (80050).",
"status": "success"
}
A single node can be stopped:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1/stop

Here the output:

{
"code": 200,
"message": "Node stopped (80051).",
"status": "success"
}
Wipe one or all nodes configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/wipe

An authenticated user can wipe all configured nodes in a lab:

{
"code": 200,
"message": "Nodes cleared (80052).",
"status": "success"
}
Wiping means delete all user config, included startup-config, VLANs, and so on. Next start will rebuild node from selected image. A single node can be wiped:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1/wipe

Here the output:

{
"code": 200,
"message": "Node cleared (80053).",
"status": "success"
}
Export one or all nodes configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/export

An authenticated user can export all configured nodes in a lab:

{
"code": 200,
"message": "Nodes exported (80057).",
"status": "success"
}
Exporting means save startup-config into the lab file. Starting a node after a wipe will load the previously exported startup-config. A single node can be wiped:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1/export

Here the output:

{
"code": 200,
"message": "Node exported (80058).",
"status": "success"
}
Get configured intefaces from a node
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/nodes/1/interfaces

An authenticated user can retrieve all configured interfaces from a node:

{
"code": 200,
"data": {
"ethernet": [
{
"name": "Gi0/0",
"network_id": 1
},
{
"name": "Gi0/1",
"network_id": 3
},
{
"name": "Gi0/2",
"network_id": 5
},
{
"name": "Gi0/3",
"network_id": 0
}
],
"serial": []
},
"message": "Successfully listed node interfaces (60030).",
"status": "success"
}
Get the lab topology
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/topology

An authenticated user can get lab topology:

{
"code": "200",
"data": [
{
"destination": "network1",
"destination_label": "",
"destination_type": "network",
"source": "node1",
"source_label": "Gi0/0",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "network1",
"destination_label": "",
"destination_type": "network",
"source": "node2",
"source_label": "Gi0/0",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "network1",
"destination_label": "",
"destination_type": "network",
"source": "node3",
"source_label": "e0/0",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "node1",
"destination_label": "Gi0/1",
"destination_type": "node",
"source": "node3",
"source_label": "e0/1",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "node1",
"destination_label": "Gi0/2",
"destination_type": "node",
"source": "node3",
"source_label": "e0/2",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "network1",
"destination_label": "",
"destination_type": "network",
"source": "node4",
"source_label": "e0/0",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "node2",
"destination_label": "Gi0/1",
"destination_type": "node",
"source": "node4",
"source_label": "e0/1",
"source_type": "node",
"type": "ethernet"
},
{
"destination": "node2",
"destination_label": "Gi0/2",
"destination_type": "node",
"source": "node4",
"source_label": "e0/2",
"source_type": "node",
"type": "ethernet"
}
],
"message": "Topology loaded",
"status": "success"
}
Get one or all pictures configured in a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/pictures

An authenticated user can get all configured pictures in a lab:

{
"code": 200,
"data": {
"1": {
"height": 201,
"id": 1,
"name": "RR Logo",
"type": "image/png",
"width": 410
}
},
"message": "Successfully listed pictures (60028).",
"status": "success"
}
A single picture can be retrieved:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/pictures/1

Here the output:

{
"code": 200,
"data": {
"height": 201,
"id": "1",
"map": "<area shape='circle' coords='248,66,30' href='telnet://:'>\n",
"name": "RR Logo",
"type": "image/png",
"width": 410
},
"message": "Picture loaded",
"status": "success"
}
The data picture can be retrieved using a different request:

curl -s -c /tmp/cookie -b /tmp/cookie
-X GET -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Lab%201.unl/pictures/1/data/32/32

The resized picture is generated with original aspect-ratio using given values as maximum witdh/lenght.

Create a new lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X POST -d '{"path":"/User1/Folder 3","name":"New
Lab","version":"1","author":"User1 Lastname","description":"A new demo
lab","body":"Lab usage and guide"}' -H 'Content-type: application/json'
http://127.0.0.1/api/labs

An authenticated user can create a new lab:

{
"code": 200,
"message": "Lab has been created (60019).",
"status": "success"
}
Move an existing lab to a different folder
curl -s -c /tmp/cookie -b /tmp/cookie
-X PUT -d '{"path":"/User1/Folder 2"}' -H 'Content-type:
application/json'
http://127.0.0.1/api/labs/User1/Folder%203/New%20Lab.unl/move

{
"code": 200,
"message": "Lab moved (60035).",
"status": "success"
}
Edit an existing lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X PUT -d '{"name":"Different
Lab","version":"2","author":"AD","description":"A different demo lab"}'
-H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Folder%202/New%20Lab.unl

An authenticated user can edit an existing lab:

{
"code": 200,
"message": "Lab has been saved (60023).",
"status": "success"
}
The request can set only one single parameter. Optional paramiter can be reverted to the default setting an empty string “”.

Add a new network to a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X POST -d '{"type":"bridge","name":"Core
Network","left":"35%","top":"25%"}' -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Folder%202/Different%20Lab.unl/networks

An authenticated user can add a network to an existing lab:

{
"code": 201,
"message": "Network has been added to the lab (60006).",
"status": "success"
}
Parameters:

left: mergin from left, in percentage (i.e. 35%), default is a random value between 30% and 70%;
name: network name (i.e. Core Network), default is NetX (X = network_id);
top: margin from top, in percentage (i.e. 25%), default is a random value between 30% and 70%;
type (mandatory): see “List network types”.
Add a new node to a lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X POST -d
'{"type":"qemu","template":"vios","config":"Unconfigured","delay":0,"icon":"Router.png","image":"vios-adventerprisek9-m-15.5.3M","name":"Core
Router
1","left":"35%","top":"25%","ram":"1024","console":"telnet","cpu":1,"ethernet":2,"uuid":"641a4800-1b19-427c-ae87-4a8ee90b7790"}'
-H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Folder%202/Different%20Lab.unl/nodes

An authenticated user can add a node to an existing lab:

{
"code": 201,
"message": "Lab has been saved (60023).",
"status": "success"
}
Parameters:

config: can be Unconfigured or Saved, default is Unconfigured;
delay: seconds to wait before starting the node, default is 0;
icon: icon (located under /opt/unetlab/html/images/icons/) used to display the node, default is Router.png;
image: image used to start the node, default is latest included in “List node templates”;
left: mergin from left, in percentage (i.e. 35%), default is a random value between 30% and 70%;
name: node name (i.e. “Core1”), default is NodeX (X = node_id);
ram: MB of RAM configured for the node, default is 1024;
template (mandatory): see “List node templates”;
top: margin from top, in percentage (i.e. 25%), default is a random value between 30% and 70%;
type (mandatory): can be iol, dynamips or qemu.
Parameters for IOL nodes:

ethernet: number of ethernet porgroups (each portgroup configures four interfaces), default is 2;
nvram: size of NVRAM in KB, default is 1024;
serial: number of serial porgroups (each portgroup configures four interfaces), default is 2.
Parameters for Dynamips nodes:

idlepc: value used for Dynamips optimization (i.e. 0x80369ac4), default is 0x0 (no optimization);
nvram: size of NVRAM in KB, default is 1024;
slot[0-9]+: the module configured in a specific slot (i.e. slot1=NM-1FE-TX).
Parameters for QEMU nodes:

console: can be telnet or vnc, default is telnet;
cpu: number of configured CPU, default is 1;
ethernet: number of ethernet interfaces, default is 4;
uuid: UUID configured, default is a random UUID (i.e. 641a4800-1b19-427c-ae87-4a8ee90b7790).
Delete an existent lab
curl -s -c /tmp/cookie -b /tmp/cookie
-X DELETE -H 'Content-type: application/json'
http://127.0.0.1/api/labs/User1/Folder%202/Different%20Lab.unl

An authenticated user can delete a lab:

{
"code": 200,
"message": "Lab has been deleted (60022).",
"status": "success"
}