import rticonnextdds_connector as rti
from os import path as osPath
from time import sleep
filepath = osPath.dirname(osPath.realpath(__file__))

connector = rti.Connector("MyParticipantLibrary::Actuator",  filepath + "/DDS.xml")
button_input_DDS = connector.getInput("ActuatorSubscriber::Actuator2_button_reader")
temp_input_DDS = connector.getInput("ActuatorSubscriber::Actuator2_temp_reader")
outputDDS = connector.getOutput("ActuatorPublisher::Actuator2_writer")
changeStatus = 'Working'  # last status will be stored here
buttonState = ''
act2Status = 'Working'
temp2 = 0
 # initial first status= working
outputDDS.instance.setString("ActuatorStatus", act2Status)
outputDDS.write()
print(f'Actuator 2 status is: {act2Status}')

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
            if temp_input_DDS.samples.getString(j, "TempID") == '2':
                temp2 = temp_input_DDS.samples.getNumber(j, "TempNumber")

    if act2Status == 'Working':
        if buttonState == 'Stop': # no need to check temperature on stop button state
            act2Status = 'Stopped'
        else:
            if temp2 > 40:  # extreme temperature
                act2Status = 'Degraded'

    if act2Status == 'Degraded':
        if buttonState == 'Stop':
            act2Status = 'Stopped'
        else:
            if temp2 < 40:  # normal temperature
                act2Status = 'Working'

    if act2Status == 'Stopped':
        if buttonState == 'Start':
            act2Status = 'Working'

    if changeStatus != act2Status:
        changeStatus = act2Status  # publish upon change
        outputDDS.instance.setString("ActuatorStatus", act2Status)
        outputDDS.instance.setString("ActuatorID", '313360489')  # student ID
        outputDDS.write()
        print(f'Actuator 2 status is: {act2Status}')  # for the sake of simplicity we will print ONLY upon change






