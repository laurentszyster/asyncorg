importPackage(Packages.org.async.simple);
var prefix = 'f6a2a053065fd9d4cccf2cdbdb2cbd95360ff3bd org.async.simple.Strings.split(String, Pattern) ';
var doctest = function () {
importClass(Packages.java.util.regex.Pattern)
strings = Simple.split(
    "one\t  and  \r\n three", Pattern.compile("\\s+(and|or)\\s+")
    );
return (
    strings.next() == "one" &&
    strings.next() == "and" &&
    strings.next() == "three"
    );
};
var run_test = function () {
    return (doctest()===true);
};