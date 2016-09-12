<?php
	
	// Check if data is set
	if (!isset($_POST['data'])) {
		exit("ERROR(1): Data is not set;");
	}
	
	$data = $_POST['data'];
	
	// Decode json data
	$data = json_decode($data);
	
	// Get timestamp
	date_default_timezone_set('Europe/Belgrade');
	$timestamp = date('U');

	// Generate xml tree
	$xml = new DOMDocument("1.0", "utf-8");
	$xml_root = $xml->createElement("queue-monitoring");
	$xml_root->setAttribute('timestamp', $timestamp);
	
	// Create tree from received data
	foreach ($data as $window)
	{   
		$xml_window = $xml->createElement("window");
		$xml_window->setAttribute("id", $window->id);
		
		$xml_name = $xml->createElement("name", $window->name);
		$xml_min = $xml->createElement("min", $window->min);
		$xml_max = $xml->createElement("max", $window->max);
		
		$xml_window -> appendChild($xml_name);
		$xml_window -> appendChild($xml_min);
		$xml_window -> appendChild($xml_max);
		
		$xml_root->appendChild($xml_window);	
	}
	
	$xml->appendChild( $xml_root );
	
	// Write out xml tree to file
	$xml->save("config.xml");

	echo "OK";

?>
