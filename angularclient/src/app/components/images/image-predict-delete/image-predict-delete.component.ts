import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';

@Component({
  selector: 'app-image-predict-delete',
  templateUrl: './image-predict-delete.component.html',
  styleUrls: ['./image-predict-delete.component.css']
})
export class ImagePredictDeleteComponent {
  title = 'Confirmation if Delete of Prediction Was Successful';
  modelId: number;
  id: any;
  response:boolean | undefined;

  constructor(private service: ImagePredictionService, private route: ActivatedRoute
  ) {
    this.modelId = -1;
    this.id = -1;
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
    });
    this.route.queryParams.subscribe(s => {
      this.id = s['id'];
    });
    this.deletePrediction();
  }

  public deletePrediction(): void {
    if (this.modelId && this.id) {
      console.log('running delete for modelId ' + this.modelId + ' and id: ' + this.id)
      this.service.deletePrediction(this.id, this.modelId).subscribe(
        (response: boolean) => {
          this.response = response;
        },
        (error: HttpErrorResponse) => {
          alert(error.message);
        }
      );
    }
  }
}
