<html>
	<head>
		<title> Смештај - Тренутно на реду </title>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
		<meta http-equiv="refresh" content="5" >
		<style>
			table, th, td, tr { border: 1px solid black; }
		</style>
	</head>
	<body>		
		<table style="width:80%; margin: 0 auto;">
		<tbody>
			<tr>
				<th>Windon</th>
				<th>Next client</th>
				<th># clients in queue</th>
				<th>Flow (clients/hour)</th>
				<th>Last change</th>
			</tr>
			<?php
				
				$dom = new DOMDocument();
				$conf = new DOMDocument();
				
				// Get configuration and data file
				$dom->load("data.xml");
				$conf->load("config.xml");
				
				$xml = $dom->documentElement;
				$conf_xml = $conf->documentElement;
				
				// Use both files to generate table with data
				$xml_windows = $xml->getElementsByTagName('window');
				$conf_windows = $conf_xml->getElementsByTagName('window');
				
				foreach ($xml_windows as $xml_window) 
				{	
					foreach ($conf_windows as $conf_window) {
						if ($conf_window->getAttribute('id') == $xml_window->getAttribute('id')) {
							$name = $conf_window->getElementsByTagName('name')->item(0)->textContent;
							$name = mb_convert_encoding($name,'HTML-ENTITIES', 'utf-8');
						}
					}

					$ticket = $xml_window->getElementsByTagName('ticket')->item(0)->textContent;
					$queue = $xml_window->getElementsByTagName('queue')->item(0)->textContent;
					$statistic = $xml_window->getElementsByTagName('statistic')->item(0)->textContent;
					$timestamp = $xml_window->getElementsByTagName('timestamp')->item(0)->textContent;
					
					$time = date('H:i:s', $timestamp);
					$date = date('d/m/Y', $timestamp);

					echo "<tr>";
					echo "<td>" . $name . "</td><td style='text-align:center;'>" . $ticket . "</td><td style='text-align:center;'>" . $queue . "</td><td style='text-align:center;'>" . $statistic . "</td><td style='text-align:right;'>" . $time . "   " . $date . "</td>";
					echo "</tr>";
				}

				$timestamp=$xml->getAttribute('timestamp');
				echo "<tr><td colspan='5'>";
				echo "Last update: " . date('H:i:s  d/m/Y', $timestamp);
				echo "</td></tr>";
			?>
		</tbody>
		</table>
	</body>
</html>
