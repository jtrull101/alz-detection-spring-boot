import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';

@Component({
  selector: 'app-image-predict',
  templateUrl: './image-predict.component.html',
  styleUrls: ['./image-predict.component.css'],
})
export class ImagePredictComponent {
  title = 'Prediction on MRI';
  public prediction: ImagePrediction;
  modelId: number;
  file: File;

  constructor(
    private service: ImagePredictionService,
    private route: ActivatedRoute
  ) {
    this.modelId = -1;
    this.prediction = {} as ImagePrediction;
    this.file = {} as File;
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
    });
  }

  onFilechange(event: any) {
    this.file = event.target.files[0];
  }

  public runPredicitonOnFile(): void {
    if (this.modelId && this.file) {
      const formData = new FormData();
      formData.append('image', this.file);
      this.service.predictFromFile(formData, this.modelId).subscribe(
        (response: ImagePrediction) => {
          this.prediction = response;
        },
        (error: HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
