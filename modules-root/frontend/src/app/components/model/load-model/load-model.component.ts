
import { HttpErrorResponse, HttpClient, HttpEventType } from '@angular/common/http';
import { Component } from '@angular/core';
import { Model } from 'src/app/models/model.model';
import { ModelService } from 'src/app/services/model.service';

@Component({
  selector: 'app-load-model',
  templateUrl: './load-model.component.html',
  styleUrls: ['./load-model.component.css']
})
export class LoadModelComponent {
  title = 'Load archived Tensorflow model'
  file: File | undefined;
  model: Model | undefined;

  constructor(private service: ModelService, private http:HttpClient){}

  onFileChange(event:any) {
    this.file = event.target.files[0];
  }

  upload() {
    if (this.file) {
      const formData = new FormData();
      formData.append("file", this.file);
      this.service.load(formData).subscribe(
        response => this.model = <Model>response,
        err => alert('failure during file upload! ' + err)
      );
    }
  }
}
