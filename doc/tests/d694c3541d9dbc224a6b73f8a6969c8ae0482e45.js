importPackage(Packages.org.async.simple);
var prefix = 'd694c3541d9dbc224a6b73f8a6969c8ae0482e45 org.async.simple.Strings.join(Object, Iterator) ';
var doctest = function () {
return Simple.join(
    ", ", Simple.iter(["A", "B", "C"])
    ).equals("A, B, C");
};
var run_test = function () {
    return (doctest()===true);
};