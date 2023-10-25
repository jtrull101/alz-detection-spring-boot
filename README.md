<h1>Alzheimer's Detection CNN - Spring Boot</h1>
<br>

<h2>Description</h2>
<p>
  This Spring Boot server is primarily designed for diagnosing Alzheimer's Disease (AD) using synthetic or fictitious MRI images. Serving as a launching pad for future full-stack 
  endeavors that integrate machine learning, this project encapsulatd a total 6 weekdays, averaging 9 hours of work per day. While certaintly limited to just fictious images,
  this project provides a valuable foundation for learning, research, and innovation in the fields of Full Stack Java development and Machine Learning with Python integration.
</p>

<h2>Key Points</h2>
<ol>
  <li><b>Synthetic MRI Analysis:</b> The server specializes in analyzing synthetic MRI images to simulate the diagnosis of AD with the assistance of AI</li>
  <li><b>Educational and Research Tool:</b> This project provides a controlled environment for healthcare professionals, students and researches to realize the impact AI could provide in early diagnosis of AD</li>
  <li><b>Explore Machine Learning Integration:</b> As a starting point, this project demonstrates the potential integrations of Machine Learning into a Full Stack solution</li>
  <li><b>Use of Real Data:</b> Due to the server supporting the injection of custom saved Tensorflow models, upon the development of a model that has high accuracy with real MRI images, that model can be loaded into this site and predictions with real MRI images can be run. Note one should never take the findings of this ML model over their own medical providers</li>
</ol>


<h2>Backend Implementation</h2>
<p>
  The backend server is implemented with Spring Boot, utilizing an H2 database that persists on disk. The Rest API exposed by this server supports most basic CRUD requests
  to interact with both Tensorflow models and individual MRI prediction.
  <br> <br>
  None of the model training occurs on this Spring Boot server. While we are leveraging Amazon's DJL framework that enables Deep Learning support in Java, the models were
  all trained using the Neural Network found at the following link:
</p>
<a href="https://github.com/jtrull101/alz-mri-neural-network">Python Alzheimer's Convolutional Neural Network</a><br>


<h2>Frontend Implementation</h2>
<p>
  The frontend server is implemented with Angular and serves as a simplistic yet clean interface to interact with the Spring Boot server than the Rest API. The home page can be seen below, accessed at http://localhost:4200
</p>
<img src="images/screenshot_homepage_springboot.png">
<p>
  From the homepage, a user can navigate to one of several options under both the <b>Models</b> and <b>Predictions</b> headers. 
  In this readme, we've just shown the pages required to access to submit a prediction to the default model. To choose the default model, navigate to <b>Models</b> -> <b>Choose Active Model</b>. 
  Click one of the following options and verify the text stating 'Current Model: {ID}' updates correctly. The default model will always have ID == 1.
</p>
<img src="images/screenshot_choosemodel_springboot.png">
<p>
  Next, navigate to <b>Predictions</b> -> <b>Submit new MRI for Assessment</b> to see the next screenshot shown below. From here, choose a file to upload to the predictive model that you have chosen.
  For convenience, test images (that the default model was not trained on) are included in src/main/resources/images/Combined Dataset.zip. Unzip this and choose an image. Each fictitious MRI in this dataset
  is sorted by the Impairment level that the MRI was emulating. Because of this fact, we will have confidence if the model is perfoming as expected after uploading some MRIs and verifying their predictions
  match our actual value.
</p>
<img src="images/screenshot_predictionresult_springboot.png">


<h2>Pending Work</h2>
<ul>
  <li>Automated testing for Angular frontend</li>
  <li>Increase test coverage for Spring Boot server</li>
  <li>Angular frontend not handling exceptions from the backend</li>
  <li>Implement safer keys in the database. Currently just incrementing
  <li>Add Users - should have their own inventory of models and predictions</li>
  <li>SSL for security when dealing with real data</li>
</ul>

<h2>Open Questions</h2>
<ul>
  <li>When running predict() on an image, should we run predict for every model? (under user?)</li>
</ul>

<h3>Contact</h3>
<p>Email: jttrull0@gmail.com</p>
<a href="https://www.linkedin.com/in/jonathan--trull/">LinkedIn</a><br>
<a href="https://github.com/jtrull101">GitHub</a>
<br><br><br>
