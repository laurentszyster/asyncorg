importPackage(Packages.org.async.simple);
var prefix = 'abd6c957f630a6012c542c690617bb813052701b org.async.simple.Bytes.find(byte[], byte[], int) ';
var doctest = function () {
return Bytes.find(
    Bytes.encode("World", "UTF-8"), 
    Bytes.encode("Hello World!", "UTF-8"), 
    0
    ) == 6;
};
var run_test = function () {
    return (doctest()===true);
};