import { HttpErrorResponse } from '@angular/common/http';
import { Component} from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ImagePrediction } from 'src/app/models/image-prediction.model';
import { ImagePredictionService } from 'src/app/services/image-prediction.service';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-image-predict-random',
  templateUrl: './image-predict-random.component.html',
  styleUrls: ['./image-predict-random.component.css'],
})
export class ImagePredictRandomComponent {
  title = 'Prediction on Random MRI';
  public prediction: ImagePrediction;
  no:number|undefined;
  veryMild:number|undefined;
  mild:number|undefined;
  moderate:number|undefined;
  modelId: number | undefined;
  img:any;
  running:boolean=false;

  constructor(
    private service: ImagePredictionService,
    private modelService: ModelService,
    private route: ActivatedRoute
  ) {
    this.modelId = modelService.getModelId();
    this.prediction = {
      id:undefined
    }
  }

  public predictRandom(): void {
    if (this.modelId) {
      this.running=true;
      this.service.predictFromRandom(this.modelId).subscribe(
        (response: ImagePrediction) => {
          this.prediction = response;
          this.no = this.prediction.conf_NoImpairment;
          this.veryMild = this.prediction.conf_VeryMildImpairment;
          this.mild = this.prediction.conf_MildImpairment;
          this.moderate = this.prediction.conf_ModerateImpairment;

          const paths = this.prediction.filepath.split("/");
          const category = paths[paths.length-2];
          const filename = paths[paths.length-1];
          this.img = "assets/test/" + category + "/" + filename;
          this.running=false;
        },
        (error: HttpErrorResponse) => {
          this.running=false;
          alert(error.message);
        }
      );
    }
  }
}
