var asyncorg = Packages.org.async;
 
importClass(asyncorg.core.DNS);
importClass(asyncorg.protocols.JSON);
importClass(asyncorg.prototypes.Stateful);

function Open(filename) {
	var loop = this;
	loop.log(JSON.pprint(DNS.servers()));
	var logResolved = Stateful.fun(function (request) {
		 loop.log(JSON.pprint(JSON.reflect(request)));
	     });
    var logConnectedAndResolveMore = Stateful.fun(function (request) {
         if (DNS.resolver.connected()) {
             DNS.resolver.log("DNS client disconnected");
         }
         DNS.resolve("NS google.com", logResolved);
         DNS.resolve("MX google.com", logResolved);
         DNS.resolve("TXT www.dnsexit.com", logResolved);
         DNS.resolve("PTR 190.19.46.207.in-addr.arpa", logResolved);
         return -1;
         })
    var logResolvedAndScheduleMore = Stateful.fun(function (request) {
        loop.log(JSON.pprint(JSON.reflect(request)));
        loop.timeout(3000, logConnectedAndResolveMore);
        });
	DNS.resolve("A www.microsoft.com", logResolvedAndScheduleMore);
}

function Close() {
}