importPackage(Packages.org.async.simple);
var prefix = '3ab6b02799deaf082c2cf14d4bad106867261543 org.async.simple.Objects.iter(Map, Object[]) ';
var doctest = function () {
var map = Simple.dict([
    "A", "test", 
    "B", true, 
    "C", 1, 
    "D", false
    ]);
var values = Simple.iter(map, ["A", "C", "E"]);
return (
    values.next() == "test" &&
    values.next() == 1 &&
    values.next() == null &&
    values.hasNext() == false
    );
};
var run_test = function () {
    return (doctest()===true);
};