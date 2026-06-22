import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RewardService, RewardSummary, RewardLedger } from './reward.service';

describe('RewardService', () => {
  let service: RewardService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RewardService]
    });
    service = TestBed.inject(RewardService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should fetch reward summary via GET', () => {
    const dummySummary: RewardSummary = {
      rewardAccountId: 1,
      accountId: 10,
      totalPoints: 120,
      createdAt: '2026-01-01',
      updatedAt: '2026-01-02'
    };

    service.getRewardSummary(10).subscribe(summary => {
      expect(summary).toEqual(dummySummary);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/rewards/10/summary');
    expect(req.request.method).toBe('GET');
    req.flush(dummySummary);
  });

  it('should fetch reward history via GET', () => {
    const dummyHistory: RewardLedger[] = [
      {
        id: 1,
        accountId: 10,
        transactionId: 100,
        transactionAmount: 50.0,
        pointsAwarded: 5,
        description: 'Promo',
        grantedAt: '2026-01-01'
      }
    ];

    service.getRewardHistory(10).subscribe(history => {
      expect(history.length).toBe(1);
      expect(history).toEqual(dummyHistory);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/rewards/10/history');
    expect(req.request.method).toBe('GET');
    req.flush(dummyHistory);
  });

  it('should initialize reward account via POST', () => {
    const dummySummary: RewardSummary = {
      rewardAccountId: 1,
      accountId: 10,
      totalPoints: 0,
      createdAt: '2026-01-01',
      updatedAt: '2026-01-01'
    };

    service.initializeRewardAccount(10).subscribe(summary => {
      expect(summary).toEqual(dummySummary);
    });

    const req = httpMock.expectOne('http://localhost:8080/api/v1/rewards/10/initialize');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({});
    req.flush(dummySummary);
  });
});
