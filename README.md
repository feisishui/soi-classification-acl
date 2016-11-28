# soi-classification-acl
Implementation of Esri ArcGIS Server Server Object Interceptor that restricts access based on parameters set at the web server level.

## Web tier Filter
This project assumes some customer implementation where accesses are appended as parameters to the QueryString of the request.  In this particular case, the current implementation is as follows:

"citizen" --> country of citizenship<br>
"clearances" --> clearances user has and comma separated (url encoded), e.g. C,S,TS<br>
"accesses" --> additional accesses user has and comma separated (url encoded), e.g. SI,TK,HCS<br>

Currently, the project queryStringFilter is a building block example where I currently have the following hardcoded:
<pre>sb.append("&citizen=US&clearances=C%2CS%2CTS&accesses=TK%2CSI-G%2CHCS-P");</pre>

