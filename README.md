# soi-classification-acl
Implementation of Esri ArcGIS Server Server Object Interceptor that restricts access based on parameters set at the web server level.

## Web tier Filter
This project assumes some customer implementation where accesses are appended as parameters to the QueryString of the request.  In this particular case, the current implementation is as follows:

"citizen" --> country of citizenship<br>
"clearances" --> clearances user has and comma separated (url encoded), e.g. C,S,TS<br>
"accesses" --> additional accesses user has and comma separated (url encoded), e.g. SI,TK,HCS<br>

Currently, the project queryStringFilter is a building block example where I currently have the following hardcoded:
<pre>sb.append("&citizen=US&clearances=C%2CS%2CTS&accesses=TK%2CSI-G%2CHCS-P");</pre>

## ArcGIS Server Object Interceptor
This project includes an SOI that depends on 2 configurations existing:
1.  A web tier fitler adding necessary information to the queryString of the request.
2.  A text file called "soi_acl.json" that is deployed for a particular Map Service located in the C:\arcgisserver\directories\arcgisoutput\Twitter_MapServer (for example) where the SOI will be enabled for that Map Service.

The format of the soi_acl.json should be the following, which is what minimum classification attributes must exist for a user in order to fullfill the request:
<code>
{
   "citizen":"US",
   "clearances":"TS",
   "accesses":"SI,TK,HCS"
}
</code>
