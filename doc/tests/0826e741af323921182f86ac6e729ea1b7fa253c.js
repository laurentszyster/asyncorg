importPackage(Packages.org.async.simple);
var prefix = '0826e741af323921182f86ac6e729ea1b7fa253c org.async.simple.Bytes.find(byte[], byte[], int) ';
var doctest = function () {
return Bytes.find(
    Bytes.encode("World", "UTF-8"), 
    Bytes.encode("Hello World!", "UTF-8"), 
    7
    ) == -1;
};
var run_test = function () {
    return (doctest()===true);
};