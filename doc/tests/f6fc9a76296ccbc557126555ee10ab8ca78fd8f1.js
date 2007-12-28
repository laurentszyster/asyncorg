importPackage(Packages.org.async.simple);
var prefix = 'f6fc9a76296ccbc557126555ee10ab8ca78fd8f1 org.async.simple.Bytes.find(byte[], byte[], int) ';
var doctest = function () {
return Bytes.find(
    Bytes.encode("World", "UTF-8"), 
    Bytes.encode("Hello World!", "UTF-8"), 
    5
    ) == 6;
};
var run_test = function () {
    return (doctest()===true);
};