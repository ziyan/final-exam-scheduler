<?php
// parse HTML to get course information
// Usage: ./crawler.bash | php crawler.php
//
// @author Ziyan Zhou (zxz6862)
//

class Course {
	public $id, $name, $instructor, $status, $max, $current, $schedule, $location;
	function __construct($line) {
		$words = split("\|",$line);
		$this->id=$words[0];
		$this->name=$words[1];
		$this->instructor=$words[2];
		$this->status=$words[3];
		$this->max=$words[4];
		$this->current=$words[5];
	}
}

function build_course($line) { return new Course($line); }

$content = "";
while(!feof(STDIN))
	$content=$content.fgets(STDIN);
$content=strip_tags($content);
$content=preg_replace("/^\s+/m","",$content);
$content=preg_replace("/\r\n/","\n",$content);
$content=preg_replace("/\n{1,}/","|",$content);
$content=substr($content, strpos($content, "|Days|From|To|WHERE|")+strlen("|Days|From|To|WHERE|"));
$content=substr($content, 0, strpos($content, "|Select|a new Term/Discipline|"));
$content=preg_replace("/\|([0-9]{3}\-[0-9]{2}\|)/","\n$1",$content);
$discipline=$argv[1];
$content=preg_replace("/\|/","\t",$content);
if($content!="") {
$content=preg_replace("/^/m",$discipline."-",$content);
echo $content."\n";
}
?>
