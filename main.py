import urllib, urllib.request, urllib.error, urllib.parse
import time, datetime
import xml.etree.ElementTree as ET
import re
import sys,json

inputXML = "http://192.168.0.2/path/to/xml/with/data.xml"
configURL = "http://remote.server.com/config.php"
uploadURL = "http://remote.server.com/upload.php"

# Time when service can reset all conters (some time when office is not working)
dailyResetAt = 4 # at hours / 24h format

# Service is based on pooling, so this is the period how long it should wait between two poolings
sleepDelay = 5 # seconds to wait between two iterations

# When there are not changes in the office (one client is spending too much time on the window)
# this will force the script to upload data to server so we have the information that service is running
forceUpdateInterval = 30 # seconds

# Statistic variables. It's based on one hour interval or last 20 clients 
statisticInterval = 3600 # seconds
statisticMaxSamplesCount = 20 # samples count


systemStartupTimestamp = time.time()
lastUpdateTimestamp = 0

windowsCount = 0
windows = {}

# ---------------------------------------------------------------
#   Classes

class Window:
	def __init__(self,name,min,max):
		self.name = name
		self.min = min
		self.max = max
		self.ticket = ""
		self.queue = 0
		self.timestamp = str2int('%d' % round(time.time()))
		self.statistic_data = []
		self.statistic = 0
	def name(self):
		return self.name
	def min(self):
		return self.min
	def max(self):
		return self.max
	def ticket(self):
		return self.ticket
	def queue(self):
		return self.queue
	def timestamp(self):
		return self.timestamp
	def statistic_data(self):
		return self.statistic_data
	def statistic(self):
		return self.statistic

# ---------------------------------------------------------------
#   System

# Returns 1 if it is time to reset the system
def isDailySystemRestart():
	global dailyResetAt
	now = datetime.datetime.now()
	t1 = now.replace(hour=dailyResetAt, minute=0, second=0, microsecond=0)
	t2 = now.replace(hour=dailyResetAt, minute=0, second=10, microsecond=0)
	if (now > t1 and now < t2):
		return 1
	return -1

# Resets all system settings
def resetSystemVariables():
	global systemStartupTimestamp, lastUpdateTimestamp
	global windowsCount, windows
	global statisticData
	
	print ("Restarting system now...")
	
	systemStartupTimestamp = time.time()
	lastUpdateTimestamp = 0
	windowsCount = 0
	windows = {}
	
	try:
		xml = getCurrentState(inputXML)
		if(xml == -1):
			print ("Unable to retrieve XML error!")
			sys.exit()
		result = configureWindows(xml);
		if (result < 0):
			print("Processing XML error: " + str(result));
			sys.exit()
		uploadConfiguration()
	except:
		print ("Crap happend. Exit.")
		sys.exit()

# Function for parsing strings into integers 
def str2int(str):
	return (-1 if str[0] == '-' else 1) * int(re.sub("\D","",str))

# Returns json string for server configuration
def getConfigJSON():
	global windows
	return json.dumps([{'id':i,
						'name':windows[i].name.decode("utf-8"), 
						'min':windows[i].min, 
						'max':windows[i].max} 
						for i in windows])

# Returns json string for server update
def getUpdateJSON():
	global windows
	return json.dumps([{'id':i,
						'ticket':windows[i].ticket, 
						'queue':windows[i].queue, 
						'statistic':windows[i].statistic,
						'timestamp':windows[i].timestamp} 
						for i in windows])

# ---------------------------------------------------------------
#   Communication to local ticketing machine
 
# Function for fetching xml from remote url
def getCurrentState(url):
	try:
		request = urllib.request.Request(url)
		response = urllib.request.urlopen(request, timeout=3)
		return response
	except:
		return -1


# These functions work with predefined XML structure
# by the ticketing machine in the waiting room of Student Center.

# -2 : caught error
# -1 : no windows detected
#  0 : no changes
def configureWindows(xml):
	global windows, windowsCount
	try:
	
		tree = ET.parse(xml)
		root = tree.getroot()
	
		lst = root.findall("rgroups/rgroup")

		if (len(lst) > 0):
			windowsCount = len(lst)
		else:
			return -1
		
		for rgroup in lst:
			id = str2int(rgroup.get("id"))
			name = "n/a"
			min = "n/a"
			max = "n/a"

			for item in rgroup:
				if (item.tag == "name"):
					name = item.text.encode('utf-8')
				if (item.tag == "min"):
					min = item.text
				if (item.tag == "max"):
					max = item.text
			windows[id] = Window(name, min, max) 

		return 0
	except:
		return -2
	
 
# -1 : caught error
#  0 : no changes
# >0 : number of changes	
def updateWindowsState(xml):	
	global windows
	try:
		changes = 0;
		tree = ET.parse(xml)
		root = tree.getroot()
		lst = root.findall("rgroups/rgroup")
	
		for rgroup in lst:
		
			id = str2int(rgroup.get("id"))
		
			nextTicketOnVendingMachine = "0"

			for item in rgroup:
				if (item.tag == "next"):
					nextTicketOnVendingMachine = item.text # temp
				if (item.tag == "queue"):
					if (item.text) :
						windows[id].queue = str2int(item.text)
					else :
						windows[id].queue = 0
		
			ticket = "XXX"
			if (nextTicketOnVendingMachine.isdigit()):
				ticket = str(str2int(nextTicketOnVendingMachine) - windows[id].queue).zfill(3)
			else:
				ticket = nextTicketOnVendingMachine[0:1] + str(str2int(nextTicketOnVendingMachine[1:3]) - windows[id].queue).zfill(2)
		
			# Very important when too many people in the queue so the numbers starts from beginning
			# Works only for one looping
			if (ticket < windows[id].min):
				if (nextTicketOnVendingMachine.isdigit()):
					ticket = str(str2int(ticket) + str2int(windows[id].max) - str2int(windows[id].min)).zfill(3)
				else:
					ticket = str(str2int(ticket[1:3]) + str2int(windows[id].max[1:3]) - str2int(windows[id].min[1:3])).zfill(2)
			
			if (windows[id].ticket != ticket):
				windows[id].ticket = ticket
			
				windows[id].statistic_data.append(str2int('%d' % round(time.time())) - windows[id].timestamp)
				if (len(windows[id].statistic_data) > statisticMaxSamplesCount):
					windows[id].statistic_data.pop(0)

				if (sum(windows[id].statistic_data) != 0):
					windows[id].statistic = round( statisticInterval * len(windows[id].statistic_data) / sum(windows[id].statistic_data) , 2)
				else:
					windows[id].statistic = 0
			
				windows[id].timestamp = str2int('%d' % round(time.time()))
				changes = changes + 1
			
		return changes
	except:
		return -1

# ---------------------------------------------------------------
#   Communication to server

def uploadConfiguration():
	global configURL

	data = getConfigJSON()
	
	while (1):
		code = uploadToServer(configURL, data)
		if (code == 0): 
			print("Configuration uploaded sucessfuly!")
			return 0
		else:
			print("Configuration upload failed: " + str(code) + ". Repeat.")
			
def uploadData():
	global lastUpdateTimestamp, uploadURL

	data = getUpdateJSON()
	
	failedUploadsCount = 0
	while (1):
		code = uploadToServer(uploadURL, data)
		
		if (code == 0): 
			lastUpdateTimestamp = time.time()
			print("Data upload done")
			break
		elif(failedUploadsCount >= 3):
			print("Skip this upload..")
			break
		else:
			failedUploadsCount = failedUploadsCount + 1
			print("Upload failed: " + str(code) + ". Repeat.")
			
def uploadToServer(url, data):
	values = {'data' : data}
	
	try:
		data = urllib.parse.urlencode(values)
		data = data.encode('utf-8')
		request = urllib.request.Request(url)
		response = urllib.request.urlopen(request, data, timeout=3)
	except:
		return -1

	rcvData = response.read().decode('utf-8')
	
	if (rcvData[0:2] == "OK"):
		return 0
	elif (rcvData[0:5] == "ERROR"): 
		return rcvData[0:rcvData.find(";")]
	
	return rcvData

# ---------------------------------------------------------------
#   Main

# At the very beginning it's necessary to reset all system variables
resetSystemVariables()

while(1):
	
	# Reset system variables if it is specified time
	if (isDailySystemRestart() == 1) : 
		resetSystemVariables()
	
	# Fetch new state from the ticketing machine
	xml = getCurrentState(inputXML);
	
	# If data received
	if (xml != -1):
		# Update windows with new state
		changes = updateWindowsState(xml)
		print("Changes: " + str(changes))
		# Check if changes exist or is it time to update anyway
		if (changes > 0 or abs(time.time() - lastUpdateTimestamp) > forceUpdateInterval):
			# Upload (new) data
			uploadData()
	else:
		print("Invalid xml data");
	
	# Sleep for some period
	time.sleep(sleepDelay)

