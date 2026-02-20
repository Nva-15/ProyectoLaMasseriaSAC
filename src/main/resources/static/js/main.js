(function ($) {
    "use strict";
    
    // Dropdown on mouse hover
    $(document).ready(function () {
        const toggleNavbarMethod = function () {
            if ($(window).width() > 992) {
                $('.navbar .dropdown').hover(
                    function () {
                        $(this).addClass('show');
                        $(this).find('.dropdown-toggle').attr('aria-expanded', 'true');
                        $(this).find('.dropdown-menu').addClass('show');
                    },
                    function () {
                        $(this).removeClass('show');
                        $(this).find('.dropdown-toggle').attr('aria-expanded', 'false');
                        $(this).find('.dropdown-menu').removeClass('show');
                    }
                );  
            } else {
                $('.navbar .dropdown').off('mouseover').off('mouseout');
            }
        };
        
        toggleNavbarMethod();
        $(window).resize(toggleNavbarMethod);
    });
    
    // Back to top button
    $(window).scroll(function () {
        if ($(this).scrollTop() > 100) {
            $('.back-to-top').fadeIn('slow');
        } else {
            $('.back-to-top').fadeOut('slow');
        }
    });
    
    $('.back-to-top').click(function () {
        $('html, body').animate({scrollTop: 0}, 1500, 'easeInOutExpo');
        return false;
    });
    
    // Testimonials carousel - Versión simplificada
    $(document).ready(function() {
        if ($('.testimonial-carousel').length > 0) {
            $('.testimonial-carousel').owlCarousel({
                loop: true,
                margin: 30,
                nav: true,
                dots: true,
                autoplay: true,
                autoplayTimeout: 5000,
                autoplayHoverPause: true,
                navText: [
                    "<i class='fa fa-chevron-left'></i>",
                    "<i class='fa fa-chevron-right'></i>"
                ],
                responsive: {
                    0: {
                        items: 1
                    },
                    768: {
                        items: 2
                    },
                    992: {
                        items: 3
                    }
                }
            });
        } else {
            console.log("No se encontró el elemento .testimonial-carousel");
        }
    });
    
})(jQuery);