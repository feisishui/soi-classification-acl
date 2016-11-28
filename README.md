# soi-classification-acl
Implementation of Esri ArcGIS Server Server Object Interceptor that restricts access based on parameters set at the web server level.

## Web tier Filter
This project assumes some customer implementation where accesses are appended to the QueryString of the request.  In this particular case, the current implementation is as follows:

&citizen=US&clearances=C%2CS%2CTS&accesses=TK%2CSI-G%2CHCS-P
