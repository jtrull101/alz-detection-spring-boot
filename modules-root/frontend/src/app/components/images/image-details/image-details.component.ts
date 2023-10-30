import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-image-details',
  templateUrl: './image-details.component.html',
  styleUrls: ['./image-details.component.css'],
})
export class ImageDetailsComponent {
  title = 'Get Details of MRI in Database';
  modelId: number | undefined;
  id: number;
  prediction: ImagePrediction | undefined;
  no: number | undefined;
  veryMild: number | undefined;
  mild: number | undefined;
  moderate: number | undefined;
  file: any;
  img: any;
  running:boolean = false;

  imageDefault = 'Input ID of Image Prediction to Get';
  constructor(
    private service: ImagePredictionService,
    private route: ActivatedRoute,
    private modelService: ModelService
  ) {
    this.id = -1;
  }

  ngOnInit(): void {
    this.route.params.subscribe((s) => {
      this.modelId = s['modelId'];
    });
    if (this.modelId == undefined) {
      this.modelId = this.modelService.getModelId();
    }
    this.route.queryParams.subscribe((s) => {
      this.id = s['id'];
    });
  }

  public getImageId(item: any): void {
    this.id = item.target.value;
  }

  public getPrediction(): void {
    if (this.modelId && this.id) {
      this.running=true;
      this.service.getPrediction(this.id, this.modelId).subscribe(
        (response: ImagePrediction) => {
          this.running=false;
          this.prediction = response;
          this.no = this.prediction.conf_NoImpairment;
          this.veryMild = this.prediction.conf_VeryMildImpairment;
          this.mild = this.prediction.conf_MildImpairment;
          this.moderate = this.prediction.conf_ModerateImpairment;
          this.file = this.prediction.filepath;
          console.log("this.file: " + this.file)

          if (this.file) {
            var paths = this.prediction.filepath.split("/");
            var category = paths[paths.length-2];
            var filename = paths[paths.length-1];
            this.img = "assets/test/" + category + "/" + filename;
          }
        },
        (error: HttpErrorResponse) => {
          this.running=false;
          alert(error.message);
        }
      );
    }
  }
}
