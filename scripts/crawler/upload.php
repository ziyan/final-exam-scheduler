<?php
// Upload parsed section information to SQL database
// Usage: ./crawler.bash | php crawler.php | php upload.php <host> <username> <password> <dbname>
// See db.sql for script to create database
// @author Ziyan Zhou (zxz6862)
//
class Section {
	public $id;
	public $title;
	public $professor;
	public $status;
	public $current;
	public $max;
	public $time;

	function __construct($fields) {
		$i = 0;
		$this->id = $fields[$i];
		$i++;
		$this->title = $fields[$i];
		$i++;
		$this->professor = $fields[$i];
		$i++;
		$this->status = strpos("Open", $fields[$i]) === FALSE ? 
				(strpos("Cance", $fields[$i]) === FALSE ? 1 : -1) : 0;
		$i++;
		if (is_numeric($fields[$i])) {
			$this->max = (int)$fields[$i];
			$i++;
		} else
			$this->max = -1;
		if (is_numeric($fields[$i])) {
			$this->current = (int)$fields[$i];
			$i++;
		} else
			$this->current = -1;
	}

	function __destruct() {
		return true;
	}

	function getQuery() {
		return "INSERT INTO obj_section (id, title, professor, status, current, max, time) VALUES ('".$this->id."','".$this->title."','".$this->professor."',".$this->status.",".$this->current.",".$this->max.",'')";
	}
}

$db = @mysql_connect($argv[1], $argv[2], $argv[3], true);
if(!$db)
	die("Failed to connect to server");

if(!@mysql_select_db($argv[4], $db))
	die("Failed to select database");

while(!feof(STDIN)) {
// get a new line
$line = fgets(STDIN);

// strip the \n in the end
if (substr($line,strlen($line)-1,1)=='\n')
	$line = substr($line, 0, strlen($line)-1);

if (empty($line))
	continue;

// split the line using tab
$fields = split("\t", $line);

//var_dump($fields);

$section = new Section($fields);

$query = $section->getQuery();


echo $query . "\n";

@mysql_query($query, $db);
}


?>
