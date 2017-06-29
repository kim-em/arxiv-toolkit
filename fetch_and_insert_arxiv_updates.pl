#!/opt/local/bin/perl
use strict;

my $perl_path = `/usr/bin/which perl`;
print STDERR "Using perl found at $perl_path"; 

use DBI;		# ** need to make sure the module DBD::mysql is also installed
use LWP::UserAgent;
use IO::All;
use Data::Dumper;

use File::Basename;
my $dirname = dirname(__FILE__);
chdir $dirname;

my $dir = 'arXiv_OAI';		# ****** directory to store output from arXiv

# *************
my ($uname, $pword) = split(/\s+/, io('arxiv_sql_password')->all);
#### expects [username][white space][password]  (doesn't matter what comes after [password], so long as it starts with white space)

my $dbh = DBI->connect("DBI:mysql:database=mathematicsliteratureproject;host=mysql.tqft.net",
                         $uname, $pword,
                         {'RaiseError' => 1, 'AutoCommit' => 1, });

# figure out when we last updated
my $latest_date = $dbh->selectrow_array("select max(updated) from arxiv");

print "latest_date: $latest_date\n";

# download the files from arXiv
get_recent_arxiv_oai($dir, $latest_date);

# parse the files and put them in the DB
insert_recent_arxiv_oai($dir, $latest_date);




sub insert_recent_arxiv_oai {
	my ($indir, $from_date) = @_;

	# sql for inserting new record
	my $insert_sth = $dbh->prepare(qq{INSERT INTO arxiv (arxivid, created, updated, authors, title, categories, comments, proxy, reportno, 
						mscclass, acmclass, journalref, doi, license, abstract) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)});

	# sql for updating existing record
	my $update_sth = $dbh->prepare(qq{update arxiv set created=?, updated=?, authors=?, title=?, categories=?, comments=?, proxy=?, reportno=?, 
						mscclass=?, acmclass=?, journalref=?, doi=?, license=?, abstract=? where arxivid=?});

	my ($ntot, $nnew, $nupdate, $ndel) = (0, 0, 0, 0);
	for my $ff (@{io($indir)}) {		# loop through all files in the directory
		#last if (-e $stop_file);
		next unless ($ff =~ /$from_date-(\d+)\.xml$/);		# filter the files
		my $sn = $1;	
		print "$ff ...\n";
		my $xml = $ff->all;		# read a giant XML blob
		while ($xml =~ m{<record>(.*?)</record>}isg) {		# loop through <record>..</record>
			my $rec = $1;
			++$ntot;
			if ($rec =~ m{<header\s*status="deleted">}si) {
				++$ndel;
				next;
			}
			my $hr = {};
			if (@{$hr}{qw(arxivid created updated authors title categories comments proxy reportno mscclass acmclass journalref doi license abstract)} =
							$rec =~ m{ ^ \s*
								<header>.*?</header> \s*
								<metadata> \s* <arXiv[^<>]*> \s*
									<id>\s*(.*?)\s*</id> \s*
									<created>\s*(.*?)\s*</created> \s*
									(?: <updated>\s*(.*?)\s*</updated> \s* )?
									<authors>\s*(.*?)\s*</authors> \s*
									<title>\s*(.*?)\s*</title> \s*
									<categories>\s*(.*?)\s*</categories> \s*
									(?: <comments>\s*(.*?)\s*</comments> \s* )?
									(?: <proxy>\s*(.*?)\s*</proxy> \s* )?
									(?: <report-no>\s*(.*?)\s*</report-no> \s* )?
									(?: <msc-class>\s*(.*?)\s*</msc-class> \s* )?
									(?: <acm-class>\s*(.*?)\s*</acm-class> \s* )?
									(?: <journal-ref>\s*(.*?)\s*</journal-ref> \s* )?
									(?: <doi>\s*(.*?)\s*</doi> \s* )?
									(?: <license>\s*(.*?)\s*</license> \s* )?
									<abstract>\s*(.*?)\s*</abstract> \s*
								</arXiv>\s* </metadata> \s*
									 $ }xis) { 
				my $nn = $dbh->selectrow_array("select count(*) FROM arxiv where arxivid = '$hr->{arxivid}'");		# check whether this arxivid is already there
				if ($nn) {		# already there -- update
					print "$hr->{arxivid} -- update\n";
					++$nupdate;
					$update_sth->execute(@{$hr}{qw(created updated authors title categories comments proxy reportno mscclass acmclass journalref doi license abstract arxivid)});
				}
				else {			# not there -- insert
					print "$hr->{arxivid} --     insert\n";
					++$nnew;
					$insert_sth->execute(@{$hr}{qw(arxivid created updated authors title categories comments proxy reportno mscclass acmclass journalref doi license abstract)});
				}
			
			}
			else {		# probably won't get here
				$rec =~ s/</\n</g;
				print "NO MATCH:\n$rec\n";
				#exit;
			}
		}
	
	}

	print "ntot: $ntot, nupdate = $nupdate, nnew = $nnew\n";

}




sub get_recent_arxiv_oai {
	my ($dir, $from_date) = @_;

	my $ua = new LWP::UserAgent();
	$ua->agent("Wilbur");			# name to put in their web logs

	my $fname_base = $from_date;
	my $resumptionToken;

	my $url = "http://export.arxiv.org/oai2?set=math&verb=ListRecords&metadataPrefix=arXiv&from=$from_date";
	print "$url ...\n";
	my $res = $ua->get($url);
	if ($res->is_success()) {
		print "  ... OK\n";
		my $xml = $res->content();
		my $outname = "${fname_base}-1.xml";
		my $io = io("$dir/$outname");
		$xml > $io;			# store in a file; will be processed later
		$io->close();
		# <resumptionToken cursor="0" completeListSize="213659">492847|1001</resumptionToken>
		($resumptionToken) = `tail -7 '$dir/$outname'` =~ m{<resumptionToken[^<>]*?>([^<>]+)<}i;
		unless ($resumptionToken) {
			print "  (no resumptionToken -- done)\n";
			return;
		}
		# this only works when there's a resumption token (presumably not the usual case)
		my ($ntot) = `tail -7 '$dir/$outname'` =~ m{<resumptionToken[^<>]*?completeListSize="(\d+)"}i;
		print "  $ntot records total\n";
	}
	else {
		print "!! FAILED:\n";
		print Dumper($res);
		exit;
	}	

	while ($resumptionToken) {
		print "   (sleeping...)\n";
		sleep(2.5*60);		# not sure how long one has to wait, but 2.5 minutes has been working for a while
		my $url = "http://export.arxiv.org/oai2?resumptionToken=$resumptionToken&verb=ListRecords";
		print "$url ...\n";
		my $res = $ua->get($url);
		if ($res->is_success()) {
			print "  ... OK\n";
			my $xml = $res->content();
			my ($nn) = $resumptionToken =~ /[^0-9](\d+)$/i;
			my $outname = "${fname_base}-$nn.xml";
			my $io = io("$dir/$outname");
			$xml > $io;
			$io->close();
			($resumptionToken) = `tail -5 '$dir/$outname'` =~ m{<resumptionToken[^<>]*?>([^<>]+)<}i;
			unless ($resumptionToken) {
				print "  (no resumptionToken -- done)\n";
				last;
			}
		}
		else {
			print "!! FAILED:\n";
			print Dumper($res);
			last;
		}	
	}
}







__END__

