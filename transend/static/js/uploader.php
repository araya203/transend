<?php
$target_dir = "/home/ec2-user/transend/transend/static/fileuploads";
    if ( 0 < $_FILES['file']['error'] ) {
        echo 'Error: ' . $_FILES['file']['error'] . '<br>';
    }
    else {
        move_uploaded_file($_FILES['file']['tmp_name'], $target_dir . $_FILES['file']['name']);
    }


?>
