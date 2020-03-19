# Denis SFTP server
Simple SFTP server implemented in Java<br>
based on https://mina.apache.org/sshd-project/embedding_ssh.html 
<p>
To shutdown the server key <b>q</b> or <b>Q</b> followed by ENTER.
</p>

*server command line:<br>
====================<br>
<code>java -cp sftp/lib/sftp.jar:sftp/lib/sshd-core-1.0.0.jar:sftp/lib/slf4j-api-1.7.12.jar sftp.Server [-p=&lt;port&gt;] [-d=&lt;dir&gt;] [-h]</code><br>
@param -p=<port> (optional) Set the SFTP server's port.<br>
@param -d=<dir> (optional) Set the SFTP server's home directory.<br>
@param -h (optional) Show parameter options.<br>





