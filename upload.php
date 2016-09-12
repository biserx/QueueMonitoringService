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
		
		$xml_ticket = $xml->createElement("ticket", $window->ticket);
		$xml_queue = $xml->createElement("queue", $window->queue);
		$xml_statistic = $xml->createElement("statistic", $window->statistic);
		$xml_timestamp = $xml->createElement("timestamp", $window->timestamp);
		
		$xml_window -> appendChild($xml_ticket);
		$xml_window -> appendChild($xml_queue);
		$xml_window -> appendChild($xml_statistic);
		$xml_window -> appendChild($xml_timestamp);
			
		$xml_root->appendChild($xml_window);	
	}
	
	$xml->appendChild( $xml_root );
	
	// Write out xml tree to file
	$xml->save("data.xml");

	echo "OK";

?>
