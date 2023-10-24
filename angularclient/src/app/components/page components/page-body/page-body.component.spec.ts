import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PageBodyComponent } from './page-body.component';

describe('PageBodyComponent', () => {
  let component: PageBodyComponent;
  let fixture: ComponentFixture<PageBodyComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [PageBodyComponent]
    });
    fixture = TestBed.createComponent(PageBodyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
