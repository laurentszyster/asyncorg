importPackage(Packages.org.async.simple);
var prefix = '41641a1d8edd75f3cc8ebf6a88f1a97613062f8b org.async.simple.Bytes.find(byte[], byte[], int) ';
var doctest = function () {
return Bytes.find(
    Bytes.encode("world", "UTF-8"), 
    Bytes.encode("Hello World!", "UTF-8"), 
    0
    ) == -1;
};
var run_test = function () {
    return (doctest()===true);
};