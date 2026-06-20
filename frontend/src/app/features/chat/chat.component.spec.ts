import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ChatComponent } from './chat.component';
import { ChatApiService } from '../../core/api/chat-api.service';
import { ChatMessage } from '../../core/api/models';

describe('ChatComponent', () => {
  let fixture: ComponentFixture<ChatComponent>;
  let component: ChatComponent;
  let chatApi: jasmine.SpyObj<ChatApiService>;

  beforeEach(async () => {
    chatApi = jasmine.createSpyObj<ChatApiService>('ChatApiService', ['send']);
    chatApi.send.and.returnValue(of({ role: 'assistant', content: 'ok' } as ChatMessage));

    await TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({}) },
        },
        { provide: ChatApiService, useValue: chatApi },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ChatComponent);
    component = fixture.componentInstance;
  });

  it('formatMessage_stripsScriptTags', () => {
    const html = component.formatMessage('<script>alert(1)</script>');
    expect(html).not.toContain('<script');
    expect(html).toContain('&lt;script&gt;');
  });

  it('formatMessage_rendersFencedCodeSafely', () => {
    component.messages = [{ role: 'assistant', content: '```\n<html>\n```' }];
    fixture.detectChanges();

    const bubble = fixture.nativeElement.querySelector('.bubble-body') as HTMLElement;
    expect(bubble.querySelector('pre code')?.textContent).toContain('<html>');
    expect(bubble.querySelector('script')).toBeNull();
  });

  it('formatMessage_stripsEventHandlers', () => {
    component.messages = [{ role: 'assistant', content: '<img src=x onerror=alert(1)>' }];
    fixture.detectChanges();

    const bubble = fixture.nativeElement.querySelector('.bubble-body') as HTMLElement;
    expect(bubble.querySelector('img')).toBeNull();
    expect(bubble.textContent).toContain('onerror=alert(1)');
  });

  it('formatMessage_handlesEmptyInput', () => {
    expect(component.formatMessage('')).toBe('');
  });

  it('bubbleRowClass_and_bubbleClass_differByRole', () => {
    expect(component.bubbleRowClass({ role: 'user', content: 'hi' })).toBe('row-user');
    expect(component.bubbleRowClass({ role: 'assistant', content: 'hi' })).toBe('row-assistant');
    expect(component.bubbleClass({ role: 'user', content: 'hi' })).toBe('bubble-user');
    expect(component.bubbleClass({ role: 'assistant', content: 'hi' })).toBe('bubble-ai');
  });

  it('formatMessage_handlesNullishInput', () => {
    expect(component.formatMessage(undefined as unknown as string)).toBe('');
  });

  it('send_ignoresBlankInput', () => {
    component.userInput = '   ';
    component.send();
    expect(component.messages.length).toBe(0);
    expect(chatApi.send).not.toHaveBeenCalled();
  });

  it('send_appendsAssistantReply', () => {
    component.userInput = 'hello';
    component.send();

    expect(component.messages.map(m => m.role)).toEqual(['user', 'assistant']);
    expect(component.loading).toBe(false);
    expect(chatApi.send).toHaveBeenCalledWith('hello');
  });

  it('send_handlesApiError', fakeAsync(() => {
    chatApi.send.and.returnValue(throwError(() => new Error('timeout')));
    component.userInput = 'slow query';
    component.send();
    tick(2000);

    expect(component.messages.length).toBe(2);
    expect(component.messages[1].role).toBe('assistant');
    expect(component.messages[1].content).toContain('too long');
    expect(component.loading).toBe(false);
  }));

  it('usePrompt_setsInputAndSends', () => {
    component.usePrompt('analyze migration');

    expect(component.userInput).toBe('');
    expect(chatApi.send).toHaveBeenCalledWith('analyze migration');
  });

  it('ngOnInit_sendsQueryParamPrompt', () => {
    TestBed.resetTestingModule();
    chatApi = jasmine.createSpyObj<ChatApiService>('ChatApiService', ['send']);
    chatApi.send.and.returnValue(of({ role: 'assistant', content: 'ok' } as ChatMessage));

    TestBed.configureTestingModule({
      imports: [ChatComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { queryParams: of({ q: '  gateway strategy  ' }) },
        },
        { provide: ChatApiService, useValue: chatApi },
      ],
    }).compileComponents();

    const localFixture = TestBed.createComponent(ChatComponent);
    localFixture.detectChanges();

    expect(chatApi.send).toHaveBeenCalledWith('gateway strategy');
  });
});
