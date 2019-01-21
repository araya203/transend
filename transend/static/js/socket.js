var ipport = "35.178.176.41";
var socket = io.connect();
$(document).ready(function() {
        socket.emit('browserconnect');
        $("#qrcodecontainer").append("<a href='#'>Generate another QR code</a>");

        socket.on('file', function(filejson) {
                var filename = filejson['filename'];
                var session_id = filejson['session_id'];
                var request = new XMLHttpRequest();
                request.open('GET', '/getfile/'+session_id+'/'+filename, true)
                request.responseType = 'blob';
                var progressBar = document.getElementById("progress");
                var display = document.getElementById("display");

                request.onprogress = function(e) {
                    if (e.lengthComputable) {
                        progressBar.max = e.total;
                        progressBar.value = e.loaded;
                        display.innerText = Math.floor((e.loaded / e.total) * 100) + '%';
                    }
                };
                request.onloadstart = function(e) {
                    $("#qrcodecontainer").hide();
                    progressBar.value = 0;
                };
                request.onloadend = function(e) {
                    $("#qrcodecontainer").show();
                    $('display').hide();
                    progressBar.value = e.loaded;
                };

                request.onload = function() {
                        if(request.status === 200) {
                                var disposition = request.getResponseHeader('content-disposition');
                                var matches = /"([^"]*)"/.exec(disposition);
                                var filename2 = (matches != null && matches[1] ? matches[1] : filename);
                                var blob = new Blob([request.response], { type: 'appliaction/octet-stream' });
                                var link = document.createElement('a');
                                link.href = window.URL.createObjectURL(blob);
                                link.download = filename2;

                                document.body.appendChild(link);
                                link.click();
                                document.body.removeChild(link);
                                $("#qrcodecontainer").children('img').attr('src', 'static/images/done.jpg');
                        }
                };
                request.send();
        });

        $("a").on('click', function(event) {
                 event.preventDefault();
                 socket.emit('browserconnect');
        });

        socket.on('link', function(urljson) {
                var url = urljson['url'];
                document.location = url;
        });

        socket.on('qrpath', function(qrjson) {
                var id = socket.io.engine.id;
                var qrpath = qrjson['QR'];
                $("#qrcodecontainer").children('img').attr('src', qrpath);
        });

        socket.on('loading', function(loadjson) {
                var isloading = loadjson['isloading'];
                if(isloading == true) {
                        $("#qrcodecontainer").children('img').attr('src', 'static/images/loadinggif.gif');
                }
        });
});
