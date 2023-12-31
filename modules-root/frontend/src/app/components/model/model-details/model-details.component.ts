import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Model } from 'src/app/models/model.model';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-model-details',
  templateUrl: './model-details.component.html',
  styleUrls: ['./model-details.component.css'],
})
export class ModelDetailsComponent {
  title = 'Details for saved Tensorflow model:';
  model: Model;
  id: number | undefined;
  plotFile: any;
  img:any;

  constructor(private service: ModelService, private route: ActivatedRoute) {
    this.model = {
      id: undefined,
    };
  }

  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.id = params['id'];
    });
    if (this.id == undefined) {
      this.id = this.service.getModelId();
    }
    this.getModelDetails();
  }

  getModelDetails() {
    if (this.model.id != undefined) return;
    this.service.get(this.id).subscribe(
      (response: Model) => {
        this.model = response;

        if (this.model.seabornPlotPath!) {
          const plot = this.service.getPlot(this.id);
          let reader = new FileReader();
          plot.forEach(plt => {
            console.log("plt:" + plt)
            this.img = URL.createObjectURL(plt);
          });
        }
      },
      (error: HttpErrorResponse) => {
        alert(error.message);
      }
    );
  }
}
