import { TestBed } from '@angular/core/testing';

import { CclService } from './ccl.service';

describe('CclService', () => {
  let service: CclService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(CclService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
