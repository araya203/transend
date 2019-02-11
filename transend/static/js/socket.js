var ipport = "35.178.176.41";
var socket = io.connect();
$(document).ready(function() {
	$(function() {
                $('#send_to_phone_submit').click(function() {
		    $("#qrcodecontainer_mob").show();
                    var form_data = new FormData($('#upload-file')[0]);
                    console.log(form_data);
                    $.ajax({
                        type: 'POST',
                        url: '/uploader/'+socket.io.engine.id,
                        data: form_data,
                        contentType: false,
                        cache: false,
                        processData: false,
                        success: function(data) {
				console.log(socket.io.engine.id);	
        			socket.emit('generate_qr_phone', data);
                                console.log(data);
                        },
                    });
   		    $('#upload-file')[0].reset();
                });
        });

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
        $('input:file').change(
            function(){
                if ($(this).val()) {
                    $('#send_to_phone_submit').show();
                    // or, as has been pointed out elsewhere:
                    //                     // $('input:submit').removeAttr('disabled'); 
                } 
            });
	
       $("#topcbutton").on('click', function(event) {
		 console.log("CLICKED TO PC");
                 event.preventDefault();
                 socket.emit('generate_qr_pc');
		 document.getElementById("progress").value = "0";
        });

       $("#tophonebutton").on('click', function(event) {
		 console.log("CLICKED TO MOB");
                 event.preventDefault();
		 $("#qrcodecontainer_mob").hide();
   		 $('#upload-file')[0].reset();
        });

        socket.on('link', function(urljson) {
                var url = urljson['url'];
                document.location = url;
        });

        socket.on('qrpath', function(qrjson) {
                var id = socket.io.engine.id;
                var qrpath = qrjson['QR'];
                var direction = qrjson['direction'];
		if (direction == "to_pc"){
			console.log("HERE");
	                $("#qrcodecontainer").children('img').attr('src', qrpath);
		}
		else {
			console.log("TO_MOB");
	                $("#qrcodecontainer_mob").children('img').attr('src', qrpath);
		}	
        });

	socket.on('scanned', function(loadjson) {
                var isScanned = loadjson['scanned'];
                if(isScanned == true) {
                        $("#qrcodecontainer_mob").children('img').attr('src', 'static/images/done.jpg');
                }
        });

        socket.on('loading', function(loadjson) {
                var isloading = loadjson['isloading'];
                if(isloading == true) {
                        $("#qrcodecontainer").children('img').attr('src', 'static/images/loadinggif.gif');
                }
        });
});
