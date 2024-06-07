#!/usr/bin/env perl
#
# run with:
# perl ./simple-test_v02.pl
#

use strict;
use warnings;
use IO::Socket::INET;

# auto-flush on socket
$| = 1;

# simple send to established socket with printout (no handling of 'invisible' characters) but delimiter
sub mySend {
	my ( $socket, $msg ) = @_;

	my $size = $socket->send( $msg );
	
	print "Sent ($size Bytes): <" . $msg . ">\n";
}

# simple receive from established socket with printout (no handling of 'invisible' characters) but delimiter
# 2nd parameter $readTwoLines is allowing to read two consecutive answers (trouble if Server behaves differently than expected ;)
sub myGet {
	my ( $socket, $readTwoLines ) = @_;
	
	my $data = "";
	
	$socket->recv( $data, 1024 );

	my $size = length( $data );
	
	print "Recv ($size Bytes): <" . $data . ">\n";

	if ( $readTwoLines ne "" ) {
		&myGet( $socket, "" );
	}
}

# just assuming a response after each request
sub myRequest {
    my ( $socket, $readTwoLines, $request ) = @_;
    
    &mySend( $socket, $request );    
    &myGet( $socket, $readTwoLines );

}


# Main arguments

my ( $host, $port ) = @ARGV;

print "\n\nRun Test for SIMPLE-TCP-SERVER at $host on port $port/tcp started\n";

# Create a connecting socket

my $socket = new IO::Socket::INET (
        PeerHost => $host,
        PeerPort => $port,
        Proto    => 'tcp',
    );
    
die "Cannot create socket $!\n" unless $socket;

# &myGet( $socket, "" );

# using the server

&myRequest( $socket, "", "REVERSE TEST01-OK-11111111122222222222\n" );

# first cmd ok, 2nd is empty cmmand
&myRequest( $socket, "y", "REVERSE TEST02-OK-and-ERR-\\n\\n11111111122222222222\n\n" );

# \p is disallowed - one error
&myRequest( $socket, "", "REVERSE TEST03-ERR-\\r\\n-11111111122222222222\r\n" );

# first cmd ok, \p will be first character of next command
&myRequest( $socket, "", "REVERSE TEST04-OK-\\n\\r-11111111122222222222\n\r" );

# due to the remaining \p from last request this will cause an ERR, \p\p will be first characters of next command
&myRequest( $socket, "", "REVERSE TEST05a-ERR-\\n\\r\\r-11111111122222222222\n\r\r" );

# due to the remaining \p\p from last request this will cause an ERR
&myRequest( $socket, "", "REVERSE TEST05a-ERR-\\n-11111111122222222222\n" );

# two commands in one chunk, should result in two OK responses
&myRequest( $socket, "y", "REVERSE TEST06a-OK-11111111122222222222\nREVERSE TEST06b-OK-11111111122222222222\n" );

# four requests triggering two errors for 256 and 257 useful characters
&myRequest( $socket, "", "REVERSE TEST07-OK-254-22223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD\n" );
&myRequest( $socket, "", "REVERSE TEST08-OK-255-222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD\n" );
&myRequest( $socket, "", "REVERSE TEST09-ERR-256-2222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD\n" );
&myRequest( $socket, "", "REVERSE TEST10-ERR-257-22222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD\n" );

# if you are brave, try sending a very large number of characters by removing the next # in line below
# &myRequest( $socket, "", "REVERSE TEST10-ERR-4x257-22222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD22222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD22222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD22222223333333333333334444444444444445555555555555566666666666666677777777777777777777788888888888888999999999999900000000000000AAAAAAAAAAAAAAAAAAAAAAAABBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBCCCCCCCCCCCCCCCCCCCCCCCCCCCCDDDDDDDDDDDDDDDDDDD\n" );

# Close connecting socket

$socket->close();

print "\nRun Test for SIMPLE-TCP-SERVER at $host on port $port/tcp finished\n\n";

exit;

	
