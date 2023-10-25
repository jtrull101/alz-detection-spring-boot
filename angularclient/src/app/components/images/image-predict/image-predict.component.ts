import { HttpErrorResponse, HttpEvent, HttpEventType } from '@angular/common/http';
import { Component } from '@angular/core';
// import { NgProgress, NgProgressRef } from 'ngx-progressbar';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';


@Component({
  selector: 'app-image-predict',
  templateUrl: './image-predict.component.html',
  styleUrls: ['./image-predict.component.css'],
})
export class ImagePredictComponent {
  title = 'Prediction on MRI';
  public prediction: ImagePrediction | undefined;
  no: number | undefined;
  veryMild: number | undefined;
  mild: number | undefined;
  moderate: number | undefined;
  modelId: number | undefined;
  file: any;
  img: any;
  running:boolean = false;
  // progressRef:NgProgressRef;

  constructor(
    private service: ImagePredictionService,
    private modelService: ModelService,
    // private progress: NgProgress
  ) {
    this.modelId = this.modelService.getModelId();
    // this.progressRef = progress.ref('progress')
  }

  onFilechange(event: any) {
    this.file = event.target.files[0];
  }

  public runPredictionOnFile(): void {
    if (this.modelId && this.file) {
      const formData = new FormData();
      formData.append('image', this.file);

      // this.progressRef.start();
      this.running = true;

      this.service.predictFromFile(formData, this.modelId).subscribe(
        (response: ImagePrediction) => {
          // this.progressRef.complete();
          this.prediction = response;
          this.no = this.prediction.conf_NoImpairment;
          this.veryMild = this.prediction.conf_VeryMildImpairment;
          this.mild = this.prediction.conf_MildImpairment;
          this.moderate = this.prediction.conf_ModerateImpairment;

          let reader = new FileReader();
          reader.readAsDataURL(this.file);
          reader.onload = () => {
            this.img = reader.result;
          };
          this.running = false;
        },
        (error: HttpErrorResponse) => {
          this.running = false;
          alert(error.message);
        }
      );
    }
  }
}
