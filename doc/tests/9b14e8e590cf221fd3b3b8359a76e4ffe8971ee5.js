importPackage(Packages.org.async.simple);
var prefix = '9b14e8e590cf221fd3b3b8359a76e4ffe8971ee5 org.async.simple.Strings.split(String, char) ';
var doctest = function () {
strings = Simple.split("one two three", ' ');
return (
    strings.next() == "one" &&
    strings.next() == "two" &&
    strings.next() == "three"
    );
};
var run_test = function () {
    return (doctest()===true);
};