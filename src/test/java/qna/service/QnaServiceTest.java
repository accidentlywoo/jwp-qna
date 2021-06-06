package qna.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import qna.CannotDeleteException;
import qna.domain.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QnaServiceTest {
	@Mock
	private QuestionRepository questionRepository;

	@Mock
	private DeleteHistoryService deleteHistoryService;

	@InjectMocks
	private QnaService qnaService;

	private Question question;
	private Answer answer;

	@BeforeEach
	public void setUp() {
		question = new Question("title1", "contents1").writeBy(UserTest.JAVAJIGI);
		answer = new Answer(UserTest.JAVAJIGI, question, "Answers Contents1");
		question.addAnswer(answer);
	}

	@Test
	public void delete_성공() {
		when(questionRepository.findByIdAndDeletedFalse(question.id())).thenReturn(Optional.of(question));

		assertThat(question.deleted()).isFalse();
		qnaService.deleteQuestion(UserTest.JAVAJIGI, question.id());

		assertThat(question.deleted()).isTrue();
		verifyDeleteHistories();
	}

	@Test
	public void delete_다른_사람이_쓴_글() {
		when(questionRepository.findByIdAndDeletedFalse(question.id())).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> qnaService.deleteQuestion(UserTest.SANJIGI, question.id()))
				.isInstanceOf(CannotDeleteException.class);
	}

	@Test
	public void delete_성공_질문자_답변자_같음() {
		when(questionRepository.findByIdAndDeletedFalse(question.id())).thenReturn(Optional.of(question));

		qnaService.deleteQuestion(UserTest.JAVAJIGI, question.id());

		assertThat(question.deleted()).isTrue();
		assertThat(answer.deleted()).isTrue();
		verifyDeleteHistories();
	}

	@Test
	public void delete_답변_중_다른_사람이_쓴_글() {
		Answer answer2 = new Answer(2L, UserTest.SANJIGI, QuestionTest.Q1, "Answers Contents1");
		question.addAnswer(answer2);

		when(questionRepository.findByIdAndDeletedFalse(question.id())).thenReturn(Optional.of(question));

		assertThatThrownBy(() -> qnaService.deleteQuestion(UserTest.JAVAJIGI, question.id()))
				.isInstanceOf(CannotDeleteException.class);
	}

	private void verifyDeleteHistories() {
		List<DeleteHistory> deleteHistories = Arrays.asList(
				new DeleteHistory(ContentType.QUESTION, question.id(), question.writer(), LocalDateTime.now()),
				new DeleteHistory(ContentType.ANSWER, answer.id(), answer.writer(), LocalDateTime.now())
		);
		verify(deleteHistoryService).saveAll(deleteHistories);
	}
}
