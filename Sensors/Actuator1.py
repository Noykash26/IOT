import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::Actuator",  filepath + "/DDS.xml")
button_input_DDS = connector.getInput("ActuatorSubscriber::Actuator1_button_reader")
temp_input_DDS = connector.getInput("ActuatorSubscriber::Actuator1_temp_reader")
outputDDS = connector.getOutput("ActuatorPublisher::Actuator1_writer")
changeStatus = 'Working'  # last status will be stored here
buttonState = ''
act1Status = 'Working'
temp1 = None
 # initial first status= working
outputDDS.instance.setString("ActuatorStatus", act1Status)
outputDDS.write()
print(f'Actuator 1 status is: {act1Status}')

while True:
    button_input_DDS.read()
    numOfSamples = button_input_DDS.samples.getLength() # size of 2 for each reader
    for j in range(0, numOfSamples):
        if button_input_DDS.infos.isValid(j):
            buttonState = button_input_DDS.samples.getString(j, "String")

    temp_input_DDS.read()
    numOfSamples = temp_input_DDS.samples.getLength()
    for j in range(0, numOfSamples):
        if temp_input_DDS.infos.isValid(j):
            if temp_input_DDS.samples.getString(j, "TempID") == '1':
                temp1 = temp_input_DDS.samples.getNumber(j, "TempNumber")

    if act1Status == 'Working':
        if buttonState == 'Stop':  # no need to check temperature on stop button state
            act1Status = 'Stopped'
        else:
            if temp1 is not None and (temp1 < 20 or temp1 > 40):  # extreme temperature
                act1Status = 'Degraded'

    if act1Status == 'Degraded':
        if buttonState == 'Stop':
            act1Status = 'Stopped'
        else:
            if temp1 > 20 and temp1 < 40:  # normal temperature
                act1Status = 'Working'

    if act1Status == 'Stopped':
        if buttonState == 'Start':
            act1Status = 'Working'

    if changeStatus != act1Status:  # publish upon change
        changeStatus = act1Status
        outputDDS.instance.setString("ActuatorStatus", act1Status)
        outputDDS.instance.setString("ActuatorID", '314963810')  # student ID
        outputDDS.write()
        print(f'Actuator 1 status is: {act1Status}')  # for the sake of simplicity we will print ONLY upon change
