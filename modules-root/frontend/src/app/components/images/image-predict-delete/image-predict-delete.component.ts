import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-image-predict-delete',
  templateUrl: './image-predict-delete.component.html',
  styleUrls: ['./image-predict-delete.component.css']
})
export class ImagePredictDeleteComponent {
  title = 'Delete MRI from Database';
  modelId: number|undefined;
  id: number;
  response:boolean | undefined;

  modelDefault = "Input ID of Model with Prediction";
  imageDefault = "Input ID of Image Prediction to delete";

  constructor(private service: ImagePredictionService, private route: ActivatedRoute, private modelService:ModelService
  ) {
    this.modelId = -1;
    this.id = -1;
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
    });
    if (this.modelId == undefined) {
      this.modelId = this.modelService.getModelId();
    }
    this.route.queryParams.subscribe(s => {
      this.id = s['id'];
    });
  }

  public getModelId(item:any): void {
    this.modelId = item.target.value;
  }
  public getImageId(item:any): void {
    this.id = item.target.value;
  }

  public deletePrediction(): void {
    if (this.modelId && this.id) {
      this.service.deletePrediction(this.id, this.modelId).subscribe(
        (response: boolean) => {
          this.response = response;
        },
        (error: HttpErrorResponse) => {
          this.response = false;
          alert(error.message);
        }
      );
    }
  }
}
