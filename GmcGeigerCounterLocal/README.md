<h1>GMC Geiger Counter Local</h1>
GMC Geiger Counter Local App and Driver receives the number of ionizing events detected or counts per minute (CPM), Average Count Per Minute (ACPM) and microSievert per hour (uSv) reading directly from the device, no internet connection or cloud needed. It can also upload the values to gmcmaps.com 
It has been tested on a GQ GMC-600+, but should also be compatible with the GQ GMC Geiger Counter with wifi See: https://www.gqelectronicsllc.com/support/GMC_Selection_Guide.htm<br>
<br>
<h2>Instalation</h2>
Due to device limitations a http proxy is required for data to be pushed into hubitat. nginx is documented in the smartapp but any other http proxy should work as well. Use the NGINX Config section in the smartapp to point the proxy server to the smartapp<br><br>
To test the proxy and smartapp:<br>
curl "http://{proxy ip address}/gmc"<br>
Should return:<br>
<html><head><body>Error! Geiger Counter is not found.ERR2.</body></html><br><br>
Use the user guide to setup wifi ssid and password. Once connected to wifi change the Server Website to the http proxy server listening on port 80, the URL to gmc to match the Nginx settings in the smartapp.<br>User ID is optional in general, but required to upload data to gmcmaps.com. Counter ID is required to be set to create a device in Hubitat and must be unique for each device.
https://www.gqelectronicsllc.com/GMC-600UserGuide.pdf
