/**
 * Phase 3: HTMX Helpers & Scroll Animations
 * Intersection Observer 기반 스크롤 애니메이션
 */

// Intersection Observer 설정
const observerOptions = {
    root: null, // 뷰포트를 기준으로
    rootMargin: '0px 0px -100px 0px', // 요소가 뷰포트 하단에서 100px 위에 있을 때 트리거
    threshold: 0.1 // 요소의 10%가 보이면 트리거
};

// Observer 콜백 함수
function handleIntersection(entries, observer) {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            // 요소가 화면에 들어오면 애니메이션 클래스 추가
            const element = entry.target;
            const animationType = element.dataset.scrollReveal || 'fade-in';

            // 애니메이션 타입에 따라 클래스 추가
            switch(animationType) {
                case 'fade-in':
                    element.classList.add('animate-fade-in');
                    break;
                case 'fade-in-fast':
                    element.classList.add('animate-fade-in-fast');
                    break;
                case 'fade-in-slow':
                    element.classList.add('animate-fade-in-slow');
                    break;
                case 'slide-in-left':
                    element.classList.add('animate-slide-in-left');
                    break;
                case 'slide-in-right':
                    element.classList.add('animate-slide-in-right');
                    break;
                case 'slide-in-bottom':
                    element.classList.add('animate-slide-in-bottom');
                    break;
                case 'scale-in':
                    element.classList.add('animate-scale-in');
                    break;
                default:
                    element.classList.add('animate-fade-in');
            }

            // 애니메이션이 추가되면 더 이상 관찰하지 않음 (성능 최적화)
            observer.unobserve(element);
        }
    });
}

// Observer 인스턴스 생성
const scrollObserver = new IntersectionObserver(handleIntersection, observerOptions);

// 스크롤 애니메이션 초기화 함수
function initScrollAnimations() {
    // data-scroll-reveal 속성을 가진 모든 요소를 관찰
    const revealElements = document.querySelectorAll('[data-scroll-reveal]');

    revealElements.forEach(element => {
        // 초기 상태: 투명하고 변환된 상태
        element.style.opacity = '0';

        const animationType = element.dataset.scrollReveal;
        switch(animationType) {
            case 'slide-in-left':
                element.style.transform = 'translateX(-30px)';
                break;
            case 'slide-in-right':
                element.style.transform = 'translateX(30px)';
                break;
            case 'slide-in-bottom':
            case 'fade-in':
            case 'fade-in-fast':
            case 'fade-in-slow':
                element.style.transform = 'translateY(20px)';
                break;
            case 'scale-in':
                element.style.transform = 'scale(0.95)';
                break;
        }

        // Observer에 등록
        scrollObserver.observe(element);
    });
}

// DOM이 로드되면 초기화
document.addEventListener('DOMContentLoaded', initScrollAnimations);

// HTMX로 새로운 콘텐츠가 로드될 때도 초기화
document.body.addEventListener('htmx:afterSwap', initScrollAnimations);

/**
 * 수동으로 스크롤 애니메이션 추가하는 헬퍼 함수
 * @param {HTMLElement} element - 애니메이션을 추가할 요소
 * @param {string} animationType - 애니메이션 타입 (fade-in, slide-in-left, etc.)
 */
function addScrollReveal(element, animationType = 'fade-in') {
    element.setAttribute('data-scroll-reveal', animationType);
    element.style.opacity = '0';

    switch(animationType) {
        case 'slide-in-left':
            element.style.transform = 'translateX(-30px)';
            break;
        case 'slide-in-right':
            element.style.transform = 'translateX(30px)';
            break;
        case 'slide-in-bottom':
        case 'fade-in':
        case 'fade-in-fast':
        case 'fade-in-slow':
            element.style.transform = 'translateY(20px)';
            break;
        case 'scale-in':
            element.style.transform = 'scale(0.95)';
            break;
    }

    scrollObserver.observe(element);
}

// Export for use in other scripts
window.addScrollReveal = addScrollReveal;
