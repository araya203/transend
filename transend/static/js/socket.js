var ipport = "35.178.176.41";
console.log(ipport);
$(document).ready(function() {
	var socket = io.connect();
        console.log("CONNECTED");
        socket.emit('browserconnect');
        socket.on('file', function(filejson) {
        	var filename = filejson['filename'];
                console.log(filename);
                $("#qrcodecontainer").children('img').attr('src', 'static/images/done.jpg');
                window.location.replace("http://transendtest.com/getfile/"+filename);
        });
        socket.on('qrpath', function(qrjson) {
                var qrpath = qrjson['QR'];
                console.log(qrpath);
                $("#qrcodecontainer").append("<img src='"+qrpath+"'/>");
        });
        socket.on('loading', function(loadjson) {
                var isloading = loadjson['isloading'];
                if(isloading) {
        	        console.log(isloading);
                        $("#qrcodecontainer").children('img').attr('src', 'static/images/loadinggif.gif');
                }

        });
});
