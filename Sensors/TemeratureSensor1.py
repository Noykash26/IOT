from sys import path as sysPath
import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
import random
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::TempSensor", filepath + "/DDS.xml")
outputDDS = connector.getOutput("TempPublisher::Temp1_writer")

while True:
    randomTemp1 = random.randint(10, 60)
    sensorID = '1'  # this is sensor number 1
    outputDDS.instance.setNumber("TempNumber", randomTemp1)
    outputDDS.instance.setString("TempID", sensorID)
    outputDDS.write()
    print(f'Sensor {sensorID} current temperature is: {randomTemp1}')

    sleep(1)

