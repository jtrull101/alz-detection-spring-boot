import { HttpErrorResponse } from '@angular/common/http';
import { Component } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
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
  id: any | undefined;

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
      },
      (error: HttpErrorResponse) => {
        alert(error.message);
      }
    );
  }
}
