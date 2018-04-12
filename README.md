# Introduction 
The step tracker is a simple android app for tracking step count during walking or running. The app offers a user interface with a simple pie chart to visualize your progress against the goal. The app also offers a debug interface to visualize the accelerometer signal and peak detection at near real time.
Debug view displays steps as calculated by apps algorithm along with the value from the inbuilt android step counter.

# Signal processing
There are two components to signal processing
>0. Remove the gravity component with a low pass filter . This provides some smoothing . This approach is clearly documented in the android public documentation
>https://developer.android.com/guide/topics/sensors/sensors_overview.html
>0 .A z-score or standard score time series peak detection algorithm adapted from following 
>https://stackoverflow.com/questions/22583391/peak-signal-detection-in-realtime-timeseries-data
>https://en.wikipedia.org/wiki/Standard_score
>There is no java version, so ended up creating one and adjusting it to suite the assignment.
>The raw z score is z = (x-\mu)/\Sigma where \mu is mean and \Sigma is the standard deviation. X is input.  Algorithm samples a certain number of inputs maintains an average for a mini\mum number of sampled inputs( LAG_SIZE) , it then >evaluates the next input and detects a peak if 
>>-X is within 1.5 and 3.8. This number was chosen empirically.
>>-Abs(X(i)-\mu(LAG_SIZE))>(threshold)*\Sigma(LAG_SIZE)
>>-X(i) >\mu(LAG_SIZE)
>>-The mean and standard deviation gets updated for next LAG_SIZE window and gets repeated for next set of inputs.
>>-All the peaks detected in the process are the steps detected among the inputs sampled.
>The figure 1 below shows the raw input and peaks detected during a walking experiment.

# User Interface
User interface is a simple pie chart as shown in figure 1. Motivation was to pick something simple which is easy to see digest and gives a clear view of where the user is with respect to the goals they have set. Pie chart is refreshed near real time using a shared state. 
Pie charts can also be used to convey physical intensity of the walk or run. User interface remains uncluttered. 

# Key struggles
There were lot of firsts for me in the assignment . 
>-Android
>-Java
>-Working with Sensors and basic digital signal processing. 
Hard part was to be on track and simultaneously ramp up on new learnings. The struggles however were primarily due to lack of time and nothing more. It would have been nice if lectures gave overview of some basic signal processing algorithm to make the concepts clear. I would have liked to see FFT being used to solve the problem.

# Learnings
It was a good first assignment. Learnings were
>-Android, Java
>-Sensors, noisiness and signal processing
>-Read \multiple papers on time series peak detection which enriched the understanding.

# Video recording
<video src="https://youtu.be/LKpJTM9yH5Y" width=400 controls>
</video>

